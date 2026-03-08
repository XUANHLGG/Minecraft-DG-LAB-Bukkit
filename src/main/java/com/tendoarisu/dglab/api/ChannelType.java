package com.tendoarisu.dglab.api;

import java.util.Locale;
import java.util.Objects;

public enum ChannelType {
    A(1),
    B(2);

    public static final StringEnumCodec<ChannelType> CODEC = new StringEnumCodec<>(ChannelType::byName, ChannelType::getSerializedName);

    private final int typeNumber;

    ChannelType(int typeNumber) {
        this.typeNumber = typeNumber;
    }

    public int getTypeNumber() {
        return typeNumber;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ChannelType byName(String name) {
        if (name == null) return A;
        return "b".equalsIgnoreCase(name) ? B : A;
    }

    public static final class StringEnumCodec<T> {
        public interface Parser<T> {
            T parse(String s);
        }

        public interface Serializer<T> {
            String serialize(T value);
        }

        private final Parser<T> parser;
        private final Serializer<T> serializer;

        public StringEnumCodec(Parser<T> parser, Serializer<T> serializer) {
            this.parser = Objects.requireNonNull(parser, "parser");
            this.serializer = Objects.requireNonNull(serializer, "serializer");
        }

        public T parse(String s) {
            return parser.parse(s);
        }

        public String serialize(T value) {
            return serializer.serialize(value);
        }
    }
}
