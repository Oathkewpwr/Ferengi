package edu.desu.cis.robot.service;

public record SensorSnapshot(
        double distance,
        double lineOffset,
        int    lineStatus
) {}