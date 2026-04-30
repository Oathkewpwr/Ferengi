def move_object_behavior():
    """
    Called repeatedly by the scheduler.
    Reads the ultrasonic sensor and either drives straight or arcs left.
    """
    # Read the distance — skip this cycle if the sensor is busy
    if not arbiter.acquire("ultrasonic", "MOVE_OBJECT", 50):
        return
try:
    distance = mbuild.ultrasonic2.get()
finally:
    arbiter.release("ultrasonic", "MOVE_OBJECT")

if not arbiter.acquire("motors", "MOVE_OBJECT", 50):
    return
try:
    if distance > 15:
        # AC1: Path is clear — drive straight
        mbot2.drive_speed(50, -50)
    else:
        # Object within 15 cm — attempt color classification
        ball = {"color": "None"}
        if arbiter.acquire("camera", "MOVE_OBJECT", 50, blocking=False):
            try:
                ball = detect_color(needs_light=False)
            finally:
                arbiter.release("camera", "MOVE_OBJECT")

        if ball["color"] != "None":
            # AC2: Recognized color = sample found — stop and signal retrieval
            mbot2.drive_speed(0, 0)
            cyberpi.display.show_label("SAMPLE: " + ball["color"], 16, "center")
            cyberpi.led.show("green")
            scheduler.stop_behavior("MOVE_OBJECT")
        else:
            # AC1/AC3: Obstacle or unclassifiable — back up slightly then steer around
            mbot2.drive_speed(-30, 30)   # reverse briefly
            time.sleep(0.3)
            move_and_turn(speed=40, diff=20, is_left=True)  # arc left
finally:
    arbiter.release("motors", "MOVE_OBJECT")