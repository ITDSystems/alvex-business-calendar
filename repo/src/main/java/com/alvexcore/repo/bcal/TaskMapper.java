package com.alvexcore.repo.bcal;

import org.activiti.engine.delegate.DelegateTask;

public interface TaskMapper {

    class KeyInfo
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

        public String getTaskKey() {
            return taskKey;
        }

        @Override
        public String toString()
        {
            return taskKey + SEPARATOR + processKey;
        }

        public String getFilteredProcessKey()
        {
            return processKey.replaceAll("\\W", "_");
        }

        public String getFilteredTaskKey()
        {
            return taskKey.replaceAll("\\W", "_");
        }
    }

    KeyInfo getTaskKeyInfo(DelegateTask delegateTask);
}
