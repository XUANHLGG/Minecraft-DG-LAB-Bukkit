package com.tendoarisu.dglab.script.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class DgLabPulseUtil {
    private DgLabPulseUtil() {}

    public static int convent(int frequency) {
        if (frequency <= 10) return 10;
        if (frequency <= 100) return frequency;
        if (frequency <= 600) return (frequency - 100) / 5 + 100;
        if (frequency <= 1000) return (frequency - 600) / 10 + 200;
        return 10;
    }

    public static List<String> pulse(int... args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("参数个数必须为偶数");
        }
        List<Integer> frequencies = new ArrayList<>();
        List<Integer> strengths = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0) {
                frequencies.add(args[i]);
            } else {
                strengths.add(args[i]);
            }
        }
        return pulse(frequencies, strengths);
    }

    public static List<String> pulse(List<Integer> frequencies, List<Integer> strengths) {
        List<String> pulses = new ArrayList<>();
        if (strengths.size() != frequencies.size()) {
            throw new IllegalArgumentException("强度和频率的数量必须一致");
        }

        StringBuilder frequency = new StringBuilder();
        StringBuilder strength = new StringBuilder();
        for (int i = 0; i < strengths.size(); i++) {
            if (frequencies.get(i) < 0 || frequencies.get(i) > 1000) {
                throw new IllegalArgumentException("频率必须在 0 到 1000 之间");
            }
            frequency.append("%02X".formatted(convent(frequencies.get(i))));

            if (strengths.get(i) < 0 || strengths.get(i) > 100) {
                throw new IllegalArgumentException("强度必须在 0 到 100 之间");
            }
            strength.append("%02X".formatted(strengths.get(i)));

            if ((i + 1) % 4 == 0) {
                pulses.add("" + frequency + strength);
                frequency = new StringBuilder();
                strength = new StringBuilder();
            }
        }
        if (!frequency.isEmpty() || !strength.isEmpty()) {
            pulses.add(rightPad(frequency.toString(), 8, '0') + rightPad(strength.toString(), 8, '0'));
        }
        return pulses;
    }

    private static String rightPad(String s, int len, char pad) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(pad);
        return sb.toString();
    }

    public static String toStringArray(List<String> array) {
        if (array.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            sb.append("\"").append(array.get(i)).append("\"");
            if (i < array.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static List<String> sinPulse(int frequency, int minStrength, int maxStrength, int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("持续时间必须大于 0");
        }
        List<Integer> strengths = new ArrayList<>();

        double amplitude = maxStrength - minStrength;
        double angleStep = Math.PI / (duration - 1);

        for (int i = 0; i < duration; i++) {
            double angle = i * angleStep;
            double sinValue = Math.sin(angle) * amplitude + minStrength;
            strengths.add((int) Math.round(sinValue));
        }

        return pulse(IntStream.generate(() -> frequency).limit(duration).boxed().toList(), strengths);
    }

    public static List<String> gradientPulse(int frequency, int startStrength, int endStrength, int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("持续时间必须大于 0");
        }
        List<Integer> strengths = new ArrayList<>();

        double step = (endStrength - startStrength) / (duration - 1.0);
        for (int i = 0; i < duration; i++) {
            strengths.add((int) Math.round(startStrength + step * i));
        }

        return pulse(IntStream.generate(() -> frequency).limit(duration).boxed().toList(), strengths);
    }

    public static List<String> smoothPulse(int frequency, int strength, int duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("持续时间必须大于 0");
        }

        return pulse(
                IntStream.generate(() -> frequency).limit(duration).boxed().toList(),
                IntStream.generate(() -> strength).limit(duration).boxed().toList());
    }
}
