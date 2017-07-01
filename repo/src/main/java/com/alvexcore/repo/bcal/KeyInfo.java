package com.alvexcore.repo.bcal;

public class KeyInfo
{
    private static final String SEPARATOR = "@";
    protected String processKey;
    protected String taskKey;

    public KeyInfo(String processKey, String taskKey) {
        this.processKey = processKey;
        this.taskKey = taskKey;
    }

    public static KeyInfo fromString(String key)
    {
        String[] parts = key.split(SEPARATOR);
        return new KeyInfo(parts[1], parts[0]);
    }

    public String getProcessKey() {
        return processKey;
    }

    public static String getProcessKey(String key)
    {
        return fromString(key).getProcessKey();
    }

    public String getTaskKey() {
        return taskKey;
    }

    public static String getTaskKey(String key) {
        return fromString(key).getTaskKey();
    }

    @Override
    public String toString()
    {
        return taskKey + SEPARATOR + processKey;
    }
}
