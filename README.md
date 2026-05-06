# Ferengi — A MAZE'n Robot

> An autonomous maze-navigating robot built on the BRACE robotics framework.

## Overview

Astro is an autonomous robot that navigates a maze using line-following and sensor-based decision making, identifies objects by color, collects a target sample, and returns to the insertion point — all without human intervention.

## Architecture

The system follows a two-tier client/server architecture:

| Layer | Language | Responsibility |
|-------|----------|----------------|
| Java Host | Java | FSM logic, command dispatch, sensor snapshot |
| Python Server | MicroPython | Hardware control, behavior scheduling, resource arbitration |

## Finite State Machine

| State | Trigger | Action |
|-------|---------|--------|
| CRUISE | Default | Follow line, monitor distance |
| IDENTIFY_OBJECT | Distance < 10cm | Camera color detection |
| AVOID_OBJECT | BLUE detected | Arc left around obstacle, return to line |
| MOVE_OBJECT | GREEN detected | Push object aside |
| COLLECT_SAMPLE | RED detected | Flash LED, mark sample collected |
| STOP | YELLOW + hasSample | Mission complete |

## Object Color Reference

| Color | Object | Action |
|-------|--------|--------|
| Red | Sample | Collect |
| Blue | Immovable obstacle | Avoid |
| Green | Movable obstacle | Push |
| Yellow | Insertion point | Return and stop |

## Key Design Decisions

- **hasSample flag** — prevents premature mission termination if the robot passes the insertion point before collecting the sample
- **cruisingStarted flag** — prevents repeated behavior restarts in the CRUISE state loop
- **Priority-based resource arbitration** — ensures safety-critical behaviors always preempt lower-priority ones
- **Proportional line control** — uses Quad RGB sensor status with proportional control for smooth line tracking

## Team

- Juhong Liang
- Clayton Smith
- David Morris
- AyoMosiah Odumosu
