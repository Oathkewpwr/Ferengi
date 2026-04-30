import cyberpi
import event
import time
import mbot2
import mbuild
import socket
import json
import _thread


# --- Configuration ---
ROBOT_ID = "Liang"
DISCOVERY_PORT = 9998
COMMAND_PORT = 9990
TELEMETRY_PORT = 9991
BUFFER_SIZE = 1024

# Color sign mapping (trained beforehand)
COLOR_NAMES = {
    1: "RED",
    2: "GREEN",
    3: "BLUE",
    4: "YELLOW"
}


# ============================================================
# Resource Arbiter (thread-safe, owner + priority)
# ============================================================

class ResourceArbiter:

    def __init__(self):
        self.lock = _thread.allocate_lock()
        self.resources = {}

    def acquire(self, resource, owner, priority=0, blocking=True, timeout=5.0):
        deadline = time.time() + timeout
        while True:
            with self.lock:
                r = self.resources.get(resource)
                if r is None or priority >= r["priority"]:
                    self.resources[resource] = {
                        "owner": owner,
                        "priority": priority
                    }
                    return True
            if not blocking or time.time() > deadline:
                return False
            time.sleep(0.01)

    def release(self, resource, owner):
        with self.lock:
            r = self.resources.get(resource)
            if r and r["owner"] == owner:
                del self.resources[resource]


arbiter = ResourceArbiter()


# ============================================================
# Publish / Subscribe Broker
# ============================================================

class PubSubBroker:

    def __init__(self):
        self.subscribers = {}
        self.lock = _thread.allocate_lock()

    def subscribe(self, topic, fn):
        with self.lock:
            self.subscribers.setdefault(topic, []).append(fn)

    def publish(self, topic, msg):
        with self.lock:
            subs = list(self.subscribers.get(topic, []))
        for fn in subs:
            try:
                fn(msg)
            except:
                pass


broker = PubSubBroker()


# ============================================================
# Telemetry Streamer
# ============================================================

class TelemetryStreamer:

    def __init__(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.clients = set()
        self.running = True
        broker.subscribe("telemetry", self.send)
        _thread.start_new_thread(self.loop, ())

    def stop(self):
        cyberpi.console.print("Stopping telemetry...\n")
        if arbiter.acquire("camera", "telemetry", 100, False):
            try:
                mbuild.smart_camera.close_light()
                self.running = False
            finally:
                arbiter.release("camera", "telemetry")

    def add_client(self, addr):
        self.clients.add(addr)

    def send(self, data):
        msg = json.dumps(data).encode()
        for c in self.clients:
            try:
                self.sock.sendto(msg, c)
            except:
                pass

    def loop(self):
        while self.running:
            if len(self.clients) < 1:
                time.sleep(0.01)
                continue

            telemetry = {}

            if arbiter.acquire("ultrasonic", "telemetry", 10, False):
                try:
                    telemetry["distance"] = mbuild.ultrasonic2.get()
                finally:
                    arbiter.release("ultrasonic", "telemetry")

            if arbiter.acquire("line", "telemetry", 10, False):
                try:
                    telemetry["line_offset"] = \
                        mbuild.quad_rgb_sensor.get_offset_track()
                    telemetry["line_status"] = \
                        mbuild.quad_rgb_sensor.get_line_sta()
                finally:
                    arbiter.release("line", "telemetry")

            broker.publish("telemetry", telemetry)
            time.sleep(0.01)


telemetry = TelemetryStreamer()


# ============================================================
# Behavior Scheduler (ROS-like)
# ============================================================

class BehaviorScheduler:

    def __init__(self):
        self.behaviors = {}
        self.lock = _thread.allocate_lock()

    def start_behavior(self, name, func, *args):
        with self.lock:
            if name in self.behaviors:
                return
            stop_flag = [False]
            self.behaviors[name] = stop_flag

        def loop():
            cyberpi.console.print("Behavior started:", name)
            while not stop_flag[0]:
                func(*args)
                time.sleep(0.02)
            cyberpi.console.print("Behavior stopped:", name)
            with self.lock:
                if name in self.behaviors and self.behaviors[name] is stop_flag:
                    del self.behaviors[name]

        _thread.start_new_thread(loop, ())

    def stop_behavior(self, name):
        with self.lock:
            if name in self.behaviors:
                self.behaviors[name][0] = True

    def stop_all(self):
        with self.lock:
            for name in list(self.behaviors.keys()):
                if name in self.behaviors:
                    self.behaviors[name][0] = True
        mbot2.forward(speed=0)


scheduler = BehaviorScheduler()


# ============================================================
# MBot Server
# ============================================================

class MBotServer:

    def __init__(self, scheduler):
        self.udp_socket = None
        self.tcp_server_socket = None
        self.shutdown_requested = False
        self.scheduler = scheduler

    def shutdown(self):
        self.shutdown_requested = True
        try:
            if self.udp_socket:
                self.udp_socket.close()
        except:
            pass
        try:
            if self.tcp_server_socket:
                self.tcp_server_socket.close()
        except:
            pass
        mbot2.forward(speed=0)

    def start(self):
        _thread.start_new_thread(self.udp_discovery, ())
        _thread.start_new_thread(self.tcp_server, ())

    def udp_discovery(self):
        self.udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.udp_socket.bind(("", DISCOVERY_PORT))
        cyberpi.console.print(
            "UDP Discovery listening on port " + str(DISCOVERY_PORT))

        while not self.shutdown_requested:
            data, addr = self.udp_socket.recvfrom(BUFFER_SIZE)
            try:
                msg = json.loads(data.decode())
            except:
                continue

            if msg.get("type") == "DISCOVERY" and \
               msg.get("payload", {}).get("robot") == ROBOT_ID:
                response = {
                    "type": "DISCOVERY",
                    "id": msg.get("id"),
                    "payload": {
                        "action": "ANNOUNCE",
                        "robotType": "MBOT2",
                        "robotId": ROBOT_ID,
                        "commandPort": COMMAND_PORT
                    }
                }
                self.udp_socket.sendto(json.dumps(response).encode(), addr)

    def tcp_server(self):
        self.tcp_server_socket = socket.socket(socket.AF_INET,
                                               socket.SOCK_STREAM)
        self.tcp_server_socket.setsockopt(
            socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.tcp_server_socket.bind(("", COMMAND_PORT))
        self.tcp_server_socket.listen(1)
        cyberpi.console.print(
            "TCP Command server listening on port " + str(COMMAND_PORT))

        while not self.shutdown_requested:
            conn = None
            try:
                conn, addr = self.tcp_server_socket.accept()
                cyberpi.console.print("Accepted connection from " + addr[0])
                conn.settimeout(300)

                buf = ""                              # accumulation buffer
                while True:
                    chunk = conn.recv(BUFFER_SIZE)
                    if not chunk:
                        break

                    buf += chunk.decode("utf-8")     #  append to buffer

                    while "\n" in buf:               # process all complete messages
                        line, buf = buf.split("\n", 1)
                        line = line.strip()
                        if not line:
                            continue

                        msg = json.loads(line)       # now guaranteed to be one full message
                        if msg.get("type") != "COMMAND":
                            continue

                        msg["payload"].setdefault("parameters", {})
                        msg["payload"]["parameters"]["client_ip"] = addr[0]
                        response_payload = handle_command(msg["payload"])

                        response = {
                            "type": "RESPONSE"
                            if response_payload["status"] == "OK"
                            else "ERROR",
                            "id": msg.get("id"),
                            "payload": response_payload
                        }
                        conn.sendall((json.dumps(response) + "\n").encode())

            except Exception as e:
                cyberpi.console.print("Error in TCP connection: " + str(e))
            finally:
                if conn:
                    cyberpi.console.print("Closing connection.")
                    self.scheduler.stop_all()
                    conn.close()


server = MBotServer(scheduler)


# ============================================================
# Camera Helpers
# ============================================================

def get_detected_ball():
    balls = []
    for sign_id in COLOR_NAMES:
        w = mbuild.smart_camera.get_sign_wide(sign_id)
        h = mbuild.smart_camera.get_sign_hight(sign_id)
        if w > 0 and h > 0:
            balls.append({
                "id": sign_id,
                "color": COLOR_NAMES[sign_id],
                "x": mbuild.smart_camera.get_sign_x(sign_id),
                "y": mbuild.smart_camera.get_sign_y(sign_id),
                "area": w * h
            })

    if balls:
        best = balls[0]
        for b in balls:
            if b["area"] > best["area"]:
                best = b
        return best

    return {"id": -1, "color": "None", "x": 0, "y": 0, "area": 0}


def camera_learn(mode):
    if arbiter.acquire("camera", "learn", 60):
        try:
            pause = 5
            if mode == "COLOR":
                mbuild.smart_camera.set_mode(mode="color")
                mbuild.smart_camera.open_light()
                for sign_id in COLOR_NAMES.keys():
                    cyberpi.display.show_label(COLOR_NAMES[sign_id], 16, 'center')
                    time.sleep(pause)
                    cyberpi.display.clear()
                    cyberpi.display.show_label("Learning...", 16, 'center')
                    mbuild.smart_camera.learn(sign=sign_id, t="until_button")
                    cyberpi.display.clear()
                mbuild.smart_camera.close_light()

                for sign_id in COLOR_NAMES.keys():
                    cyberpi.display.show_label(
                        "Place " + COLOR_NAMES[sign_id] + " Object", 16, 'center')
                    time.sleep(pause)
                    cyberpi.display.clear()
                    cyberpi.display.show_label(
                        "Detecting " + COLOR_NAMES[sign_id] + " Object", 16, 'center')
                    ball = get_detected_ball()
                    if COLOR_NAMES[sign_id] != ball["color"]:
                        raise AssertionError("Object color not detected")
                cyberpi.display.clear()
                cyberpi.display.show_label("Colors Learned!", 16, 'center')
                time.sleep(pause)
                cyberpi.display.clear()
                mbuild.smart_camera.close_light()
        finally:
            arbiter.release("camera", "learn")


def detect_color(needs_light=True):
    mbuild.smart_camera.set_mode(mode="color")
    if needs_light:
        mbuild.smart_camera.open_light()
    ball = get_detected_ball()
    if needs_light:
        mbuild.smart_camera.close_light()
    return ball


def get_label_from_camera(needs_light=True):
    labels = []
    mbuild.smart_camera.set_mode(mode="line")
    if needs_light:
        mbuild.smart_camera.open_light()
    for label_id in range(1, 16):
        if mbuild.smart_camera.detect_label(label_id):
            labels.append(label_id)
    if needs_light:
        mbuild.smart_camera.close_light()
    return {"labels": labels}


def center_color_object():
    THRESHOLD = 20
    is_done = False
    while not is_done:
        block = detect_color(True)
        if block["color"] != "None":
            x = block["x"]
            error = 160 - x
            if abs(error) >= THRESHOLD:
                turn(error // 10)
            else:
                is_done = True
        else:
            is_done = True


# ============================================================
# Command Registry
# ============================================================

COMMAND_HANDLERS = {}


def register_command(command_name):
    """Decorator to register a command handler.

    Usage:
        @register_command("MY_COMMAND")
        def handle_my_command(payload):
            params = payload.get("parameters", {})
            ...
            return ok_response("Done")
    """
    def decorator(func):
        COMMAND_HANDLERS[command_name] = func
        return func
    return decorator


def ok_response(message, data={}):
    return {"status": "OK", "message": message, "data": data}


def error_response(code, message):
    return {"status": "ERROR", "errorCode": code, "message": message}


def handle_command(payload):
    command = payload.get("command")
    handler = COMMAND_HANDLERS.get(command)
    if handler:
        try:
            return handler(payload)
        except Exception as e:
            cyberpi.console.print("Error handling command: " + str(e))
            return error_response("COMMAND_EXECUTION_ERROR", str(e))
    return error_response("UNKNOWN_COMMAND", "Command not supported")


# ============================================================
# Sensor Reader
# ============================================================

def read_sensor(parameters):
    sensor = parameters["sensor"]

    if sensor == "CAMERA_COLOR":
        if arbiter.acquire("camera", "CMD", 30):
            try:
                needs_light = parameters["light"] == "YES"
                return detect_color(needs_light)
            finally:
                arbiter.release("camera", "CMD")

    if sensor == "CAMERA_LABEL":
        if arbiter.acquire("camera", "CMD", 30):
            try:
                needs_light = parameters["light"] == "YES"
                return get_label_from_camera(needs_light)
            finally:
                arbiter.release("camera", "CMD")

    if sensor == "ULTRASONIC":
        if arbiter.acquire("ultrasonic", "CMD", 20):
            try:
                return {"distance_cm": mbuild.ultrasonic2.get()}
            finally:
                arbiter.release("ultrasonic", "CMD")

    if sensor == "LINE_OFFSET":
        if arbiter.acquire("line", "CMD", 30):
            try:
                return {"line_offset": mbuild.quad_rgb_sensor.get_offset_track()}
            finally:
                arbiter.release("line", "CMD")

    if sensor == "LINE_STATUS":
        if arbiter.acquire("line", "CMD", 30):
            try:
                return {"line_status": mbuild.quad_rgb_sensor.get_line_sta()}
            finally:
                arbiter.release("line", "CMD")

    if sensor == "IMU":
        if arbiter.acquire("imu", "CMD", 20):
            try:
                return {
                    "yaw":   cyberpi.get_yaw(),
                    "pitch": cyberpi.get_pitch(),
                    "roll":  cyberpi.get_roll()
                }
            finally:
                arbiter.release("imu", "CMD")

    raise ValueError("Unknown sensor")


# ============================================================
# LED Helper
# ============================================================

def handle_led(params):
    status = params["status"]

    if status == "ON":
        led_id = int(params["id"])
        if led_id < 1 or led_id > 5:
            led_id = "all"
        cyberpi.led.on(int(params["red"]), int(params["green"]),
                       int(params["blue"]), id=led_id)
        return "LED turned on"

    if status == "OFF":
        led_id = int(params["id"])
        if led_id < 1 or led_id > 5:
            led_id = "all"
        cyberpi.led.off(id=led_id)
        return "LED turned off"

    if status == "SHOW":
        cyberpi.led.show(params["color"])
        return "LED showing"

    if status == "MOVE":
        step = int(params["step"])
        cyberpi.led.move(step)
        return "LED moved " + str(step) + " step(s)"

    raise ValueError("Unknown LED status request")


# ============================================================
# Built-in Command Handlers
# ============================================================

@register_command("MOVE_STRAIGHT")
def handle_move_straight(payload):
    if arbiter.acquire("motors", "CMD_MOVE", 50):
        try:
            mbot2.straight(float(payload["parameters"]["distance"]))
            return ok_response("Move completed")
        finally:
            arbiter.release("motors", "CMD_MOVE")


@register_command("MOVE_TIME")
def handle_move_time(payload):
    params = payload["parameters"]
    if arbiter.acquire("motors", "CMD_MOVE", 50):
        try:
            t = params.get("time", -1)
            t = t if t > 0 else "null"
            speed = params.get("speed", 50)
            mbot2.forward(speed, t)
            return ok_response("Move completed")
        finally:
            arbiter.release("motors", "CMD_MOVE")


@register_command("SET_MOTOR_POWER")
def handle_set_motor_power(payload):
    if arbiter.acquire("motors", "CMD_POWER", 55):
        try:
            p = payload["parameters"]
            if p["left"] != "AS_IS":
                mbot2.EM_set_power(float(p["left"]), "EM1")
            if p["right"] != "AS_IS":
                mbot2.EM_set_power(float(p["right"]), "EM2")
            return ok_response("Power set")
        finally:
            arbiter.release("motors", "CMD_POWER")



def turn(angle):
    if angle != 180:
        angle = (angle +180) % 360 - 180 # Normalize to (-180, 180]
    kp = 0.35
    min_power = 15
    max_power = 30
    cyberpi.reset_yaw()

    while True:
        current_angle = cyberpi.get_yaw()
        if angle > 0:
            current_angle = current_angle if current_angle >= 0 else 180 + (current_angle % 180) 
        elif angle < 0:
            current_angle = current_angle if current_angle <= 0  else -180 + (current_angle % -180)
        error = angle - current_angle
        if abs(error) < 1:
            break
        power = max(-max_power, min(max_power, kp * error))
        if abs(power) < min_power:
            power = min_power if power > 0 else -min_power
        mbot2.EM_set_power(-power, "EM1")
        mbot2.EM_set_power(-power, "EM2")

    mbot2.EM_set_power(0, "EM1")
    mbot2.EM_set_power(0, "EM2")


@register_command("TURN")
def handle_turn(payload):
    if arbiter.acquire("motors", "TURN", 50):
        try:
            turn(int(payload["parameters"]["degrees"]))
            return ok_response("Turn completed")
        finally:
            mbot2.forward(speed=0)
            arbiter.release("motors", "TURN")


def move_and_turn(speed, t=0, diff=20, is_left=True):
    em1_speed = speed
    em2_speed = -speed
    if speed != 0:
        if is_left:
            em1_speed -= (abs(em1_speed) / em1_speed) * diff
        else:
            em2_speed -= (abs(em2_speed) / em2_speed) * diff
    mbot2.drive_speed(em1_speed, em2_speed)
    if t > 0:
        time.sleep(t)
        mbot2.drive_speed(0, 0)

@register_command("MOVE_AND_TURN")
def handle_move_and_turn(payload):
    if arbiter.acquire("motors", "MOVE_AND_TURN", 50):
        try:
            params = payload["parameters"]
            speed = params.get("speed",50)
            diff = params.get("diff",20)
            t = params.get("duration",0)
            is_left = params.get("is_left", False)
            move_and_turn(speed = speed, t = t, diff= diff, is_left= is_left)
            return ok_response("Turn command completed")
        finally:
            arbiter.release("motors", "MOVE_AND_TURN")


@register_command("LED")
def handle_led_command(payload):
    if arbiter.acquire("led", "LED_CMD", 50):
        try:
            msg = handle_led(payload["parameters"])
            return ok_response(msg)
        finally:
            arbiter.release("led", "LED_CMD")


@register_command("CAMERA_LED")
def handle_camera_led(payload):
    if arbiter.acquire("camera", "CAMERA_LED", 20):
        try:
            status = payload["parameters"]["status"]
            if status == "ON":
                mbuild.smart_camera.open_light()
            elif status == "OFF":
                mbuild.smart_camera.close_light()
            else:
                return error_response("UNKONWN_STATUS",
                                      str(status) + " status not known")
            return ok_response("Camera LED is " + status)
        finally:
            arbiter.release("camera", "CAMERA_LED")


@register_command("STOP")
def handle_stop(payload):
    if arbiter.acquire("motors", "STOP", 100):
        try:
            mbot2.forward(speed=0)
            return ok_response("Stopped")
        finally:
            arbiter.release("motors", "STOP")


@register_command("GET_SENSOR")
def handle_get_sensor(payload):
    try:
        return ok_response("Sensor data", read_sensor(payload["parameters"]))
    except:
        return error_response("SENSOR_NOT_AVAILABLE", "Not available")


@register_command("TELEMETRY")
def handle_telemetry(payload):
    params = payload.get("parameters", {})
    action = params.get("action", "REGISTER")
    client_ip = params.get("client_ip")
    client_port = params.get("port", 0)

    if action == "REGISTER":
        if client_ip and client_port > 0:
            telemetry.add_client((client_ip, client_port))
            return ok_response("Telemetry registered")
        return error_response("NO_CLIENT", "Client address and port required")

    if action == "DEREGISTER":
        if client_ip and client_port > 0:
            try:
                telemetry.clients.remove((client_ip, client_port))
            except KeyError:
                pass
            return ok_response("Telemetry unregistered")

    return error_response("UNKNOWN_ACTION", "Telemetry action unknown")


@register_command("STOP_BEHAVIOR")
def handle_stop_behavior(payload):
    parameters = payload.get("parameters")
    behavior_name = parameters.get("behavior_name")
    if arbiter.acquire("motors", "LINE", 180):
        mbot2.forward(speed=0)
        arbiter.release("motors", "LINE")
    scheduler.stop_behavior(behavior_name)
    return ok_response("Behavior " + behavior_name + " stopped")


@register_command("STOP_ALL_BEHAVIORS")
def handle_stop_all_behaviors(payload):
    if arbiter.acquire("motors", "LINE", 180):
        mbot2.forward(speed=0)
        arbiter.release("motors", "LINE")
    scheduler.stop_all()
    return ok_response("All Behaviors Stopped")


# ============================================================
# CyberPi Button Events
# ============================================================

@event.is_press('a')
def on_button_a_pressed():
    cyberpi.console.print("A pressed — shutting down servers")
    telemetry.stop()
    scheduler.stop_all()
    server.shutdown()


@event.is_press('b')
def learn_colors():
    camera_learn("COLOR")


# ============================================================
# Startup
# ============================================================

cyberpi.wifi.connect("CIS_WiFi", "CIS!2018#WiFi")
cyberpi.display.show_label("connecting to wifi...", 12, "center")
while not cyberpi.wifi.is_connect():
    time.sleep(0.1)
cyberpi.display.clear()
cyberpi.console.print("Starting mBot2 server...")
cyberpi.console.print("Robot ID: " + ROBOT_ID)

server.start()


# ################################################################
# EXTENSION ZONE — Add custom commands below this line
# ################################################################
#
# Use the @register_command decorator to add new commands.
# Each handler receives a `payload` dict and must return either
# ok_response(...) or error_response(...).
#
# Template:
#
#   @register_command("MY_COMMAND")
#   def handle_my_command(payload):
#       params = payload.get("parameters", {})
#       # your logic here
#       return ok_response("Done", {"key": "value"})
#
# For behaviors (looping background tasks), define a behavior
# function and start it with the scheduler:
#
#   def my_behavior():
#       # called repeatedly until stopped
#       pass
#
#   @register_command("MY_BEHAVIOR")
#   def handle_start_my_behavior(payload):
#       scheduler.start_behavior("MY_BEHAVIOR", my_behavior)
#       return ok_response("My behavior started")
#
# ################################################################

# ============================================================
# Behavior Functions
# ============================================================

def avoid_crashing_behavior(threshold):
    if not arbiter.acquire("ultrasonic", "AVOID_CRASH", 250, blocking=False):
        return
    try:
        distance = mbuild.ultrasonic2.get()
    finally:
        arbiter.release("ultrasonic", "AVOID_CRASH")

    if distance > threshold:
        return

    if not arbiter.acquire("motors", "AVOID_CRASH", 250, blocking=False):
        return
    try:
        mbot2.drive_speed(0, 0)
    finally:
        arbiter.release("motors", "AVOID_CRASH")

@register_command("AVOID_CRASHING")
def handle_avoid_crashing(payload):
    params = payload.get("parameters", {})
    threshold = params.get("threshold", 15)
    scheduler.start_behavior("AVOID_CRASHING", avoid_crashing_behavior,
                             threshold)
    return ok_response("Avoiding crashes")


def stop_at_line_behavior():
    if not arbiter.acquire("line", "STOP_AT_LINE", 150, blocking=False):
        return
    try:
        status = mbuild.quad_rgb_sensor.get_line_sta()
    finally:
        arbiter.release("line", "STOP_AT_LINE")

    if not arbiter.acquire("motors", "STOP_AT_LINE", 180, blocking=False):
        return
    try:
        if status > 0:
            mbot2.drive_speed(0,0)
    finally:
        arbiter.release("motors", "STOP_AT_LINE")


@register_command("STOP_AT_LINE")
def handle_stop_at_line(payload):
    scheduler.start_behavior("STOP_AT_LINE", stop_at_line_behavior)
    return ok_response("STOP_AT_LINE behavior started")

# ── STEER_AROUND behavior ──────────────────────────────────────────────────
def steer_around_behavior(threshold, speed, diff, is_left):
    """
    Called repeatedly by the scheduler.
    Reads the ultrasonic sensor and either drives straight or arcs left.
    """
    # Read the distance — skip this cycle if the sensor is busy
    if not arbiter.acquire("ultrasonic", "STEER_AROUND", 200, blocking=False):
        return
    try:
        distance = mbuild.ultrasonic2.get()
    finally:
        arbiter.release("ultrasonic", "STEER_AROUND")
    # Acquire the motors — skip this cycle if something higher-priority holds them
    if not arbiter.acquire("motors", "STEER_AROUND", 200, blocking=False):
        return
    try:
        if distance > threshold:
            # Path is clear — drive straight
            mbot2.drive_speed(speed, -speed)
        else:
            # Obstacle detected — arc left
            move_and_turn(speed=speed, diff=diff, is_left=is_left)
    finally:
        arbiter.release("motors", "STEER_AROUND")
@register_command("STEER_AROUND")
def handle_steer_around(payload):
    params    = payload.get("parameters", {})
    threshold = float(params.get("threshold", 25))
    speed     = float(params.get("speed",     40))
    diff      = float(params.get("diff",      20))
    is_left = bool(params.get("is_left", True))
    if speed < 0 or speed > 100:
        return error_response("INVALID_PARAM",
                              "speed must be between 0 and 100")
    if diff < 0 or diff >= speed:
        return error_response("INVALID_PARAM",
                              "diff must be >= 0 and less than speed")
    scheduler.start_behavior("STEER_AROUND", steer_around_behavior,
                             threshold, speed, diff, is_left)
    return ok_response("STEER_AROUND started")

@register_command("FLASH_LED")
def handle_flash_led(payload):
    params = payload.get("parameters", {})
    times  = int(params.get("times", 3))
    r      = int(params.get("red",   0))
    g      = int(params.get("green", 0))
    b      = int(params.get("blue",  255))
    delay  = float(params.get("delay", 0.3))
    if times < 1 or times > 20:
        return error_response("INVALID_PARAM",
                              "times must be between 1 and 20")
    if arbiter.acquire("led", "FLASH_LED", 50):
        try:
            for _ in range(times):
                cyberpi.led.on(r, g, b, id="all")
                time.sleep(delay)
                cyberpi.led.off(id="all")
                time.sleep(delay)
            return ok_response("Flash complete")
        finally:
            arbiter.release("led", "FLASH_LED")


def follow_line_behavior():

    if not arbiter.acquire("line", "FOLLOW_LINE", 10, blocking=False):
        return
    try:
        status = mbuild.quad_rgb_sensor.get_line_sta()
    finally:
        arbiter.release("line", "FOLLOW_LINE")


    if not arbiter.acquire("motors", "FOLLOW_LINE", 10, blocking=False):
        return
    try:

        kp = 0.5
        base_speed = 30

        if status == 0:
            error = 50
        elif status == 1:
            error = 0
        elif status > 1 and status < 4:
            error = -10
        elif status < 7:
            error = -20
        elif status >= 14:
            error = -75
        else:
            error = -40
        correction = error * kp
        em1_speed = base_speed + correction
        em1_speed = min(max(em1_speed, -50), 50)
        em2_speed = -base_speed + correction
        em2_speed = min(max(em2_speed, -50), 50)
        mbot2.drive_speed(em1_speed, em2_speed)
    finally:
        arbiter.release("motors", "FOLLOW_LINE")

@register_command("FOLLOW_LINE")
def handle_follow_line(payload):
    scheduler.start_behavior("FOLLOW_LINE", follow_line_behavior)
    return ok_response("Following Line")

@register_command("PUSH_OBJECT")
def handle_push_object(payload):
    if not arbiter.acquire("ultrasonic", "PUSH_OBJECT", 100):
        return error_response("RESOURCE_BUSY", "Ultrasonic busy")
    try:
        distance = mbuild.ultrasonic2.get()
    finally:
        arbiter.release("ultrasonic", "PUSH_OBJECT")

    if distance > 20:
        return ok_response("No object detected")

    if arbiter.acquire("motors", "PUSH_OBJECT", 100):
        try:
            mbot2.turn(180, wait=True)

            mbot2.forward(-40, distance * 1.3, "cm", wait=True)

            move_and_turn(speed=40, diff=20, is_left=True)
            time.sleep(3)
            mbot2.forward(0, 0, "cm", wait=True)

            mbot2.forward(40, distance * 1.3, "cm", wait=True)

            mbot2.turn(180, wait=True)

            return ok_response("Object pushed")
        finally:
            arbiter.release("motors", "PUSH_OBJECT")

    return error_response("RESOURCE_BUSY", "Motors busy")