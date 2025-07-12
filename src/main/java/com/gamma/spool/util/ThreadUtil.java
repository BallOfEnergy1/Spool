package com.gamma.spool.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class ThreadUtil {

    public static String getThreadNameById(long id) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
        return threadInfo != null ? threadInfo.getThreadName() : "null";
    }
}
