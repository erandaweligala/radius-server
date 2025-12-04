package com.csg.airtel.aaa4j.domain.model;



import jakarta.json.bind.annotation.JsonbDateFormat;

import java.util.Date;

public class CrdRequest {

    private String username;
    private String sessionId;
    private String nasIp;
    private long inputOctets;
    private long outputOctets;
    private long sessionTime;

    @JsonbDateFormat("yyyy-MM-dd HH:mm:ss") // Ensures consistent JSON format
    private Date startTime;

    @JsonbDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date stopTime;



    // Getters & Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getNasIp() {
        return nasIp;
    }

    public void setNasIp(String nasIp) {
        this.nasIp = nasIp;
    }

    public long getInputOctets() {
        return inputOctets;
    }

    public void setInputOctets(long inputOctets) {
        this.inputOctets = inputOctets;
    }

    public long getOutputOctets() {
        return outputOctets;
    }

    public void setOutputOctets(long outputOctets) {
        this.outputOctets = outputOctets;
    }

    public long getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(long sessionTime) {
        this.sessionTime = sessionTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public void setStopTime(Date stopTime) {
        this.stopTime = stopTime;
    }

    @Override
    public String toString() {
        return "CrdRequest{" +
                "username='" + username + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", nasIp='" + nasIp + '\'' +
                ", inputOctets=" + inputOctets +
                ", outputOctets=" + outputOctets +
                ", sessionTime=" + sessionTime +
                ", startTime=" + startTime +
                ", stopTime=" + stopTime +
                '}';
    }

}
