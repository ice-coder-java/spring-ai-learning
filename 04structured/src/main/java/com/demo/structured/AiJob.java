package com.demo.structured;

public class AiJob {
    record BookingInfo(String name, String bookingNumber) {}

    record Job(JobType jobType, BookingInfo keyInfos) {}

    public enum JobType {
        CANCEL,
        QUERY,
        OTHER,
    }
}
