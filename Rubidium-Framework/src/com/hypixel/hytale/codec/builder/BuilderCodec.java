package com.hypixel.hytale.codec.builder;

public interface BuilderCodec<T> {
    T decode(String data);
    String encode(T value);
    
    BuilderCodec<Boolean> BOOLEAN = new BuilderCodec<Boolean>() {
        @Override
        public Boolean decode(String data) {
            return Boolean.parseBoolean(data);
        }
        @Override
        public String encode(Boolean value) {
            return String.valueOf(value);
        }
    };
    
    BuilderCodec<String> STRING = new BuilderCodec<String>() {
        @Override
        public String decode(String data) {
            return data;
        }
        @Override
        public String encode(String value) {
            return value;
        }
    };
    
    BuilderCodec<Integer> INTEGER = new BuilderCodec<Integer>() {
        @Override
        public Integer decode(String data) {
            try {
                return Integer.parseInt(data);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        @Override
        public String encode(Integer value) {
            return String.valueOf(value);
        }
    };
    
    BuilderCodec<Float> FLOAT = new BuilderCodec<Float>() {
        @Override
        public Float decode(String data) {
            try {
                return Float.parseFloat(data);
            } catch (NumberFormatException e) {
                return 0f;
            }
        }
        @Override
        public String encode(Float value) {
            return String.valueOf(value);
        }
    };
    
    BuilderCodec<Double> DOUBLE = new BuilderCodec<Double>() {
        @Override
        public Double decode(String data) {
            try {
                return Double.parseDouble(data);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        @Override
        public String encode(Double value) {
            return String.valueOf(value);
        }
    };
    
    BuilderCodec<Void> VOID = new BuilderCodec<Void>() {
        @Override
        public Void decode(String data) {
            return null;
        }
        @Override
        public String encode(Void value) {
            return "";
        }
    };
    
    static <T> BuilderCodec<T> of(Class<T> clazz) {
        return new BuilderCodec<T>() {
            @Override
            public T decode(String data) {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return null;
                }
            }
            
            @Override
            public String encode(T value) {
                return "{}";
            }
        };
    }
}
