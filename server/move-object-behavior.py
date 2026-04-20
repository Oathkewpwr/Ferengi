def move_object_behavior():

    # Read the distance — skip this cycle if the sensor is busy
    if not arbiter.acquire("ultrasonic", "MOVE_OBJECT", 50):
        return

    try:
        distance = mbuild.ultrasonic2.get()
    finally:
        arbiter.release("ultrasonic", "MOVE_OBJECT")

    # Acquire the motors — skip this cycle if something higher-priority holds them
    if not arbiter.acquire("motors", "MOVE_OBJECT", 50):
        return

    try:
        // TODO: add logic here
    finally:
        arbiter.release("motors", "MOVE_OBJECT")


@register_command("MOVE_OBJECT")
def handle_move_object(payload):
    move_object_behavior()
    return ok_response("STEER_AROUND started")