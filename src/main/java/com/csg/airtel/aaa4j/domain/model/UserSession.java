package com.csg.airtel.aaa4j.domain.model;

import java.time.Instant;

public class UserSession {
    private Long id;
    private String sessionId;
    private String username;
    private String nasIP;
    private String nasPortId;
    private String framedIPAddress;
    private Long inputOctets;
    private Long outputOctets;
    private Long sessionTime;
    private String status; // ACTIVE, STOPPED
    private Instant startTime;
    private Instant lastUpdateTime;
    private Instant endTime;

    public UserSession() {}

    public UserSession(String sessionId, String username, String nasIP) {
        this.sessionId = sessionId;
        this.username = username;
        this.nasIP = nasIP;
        this.status = "ACTIVE";
        this.startTime = Instant.now();
        this.lastUpdateTime = Instant.now();
        this.inputOctets = 0L;
        this.outputOctets = 0L;
        this.sessionTime = 0L;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNasIP() { return nasIP; }
    public void setNasIP(String nasIP) { this.nasIP = nasIP; }

    public String getNasPortId() { return nasPortId; }
    public void setNasPortId(String nasPortId) { this.nasPortId = nasPortId; }

    public String getFramedIPAddress() { return framedIPAddress; }
    public void setFramedIPAddress(String framedIPAddress) { this.framedIPAddress = framedIPAddress; }

    public Long getInputOctets() { return inputOctets; }
    public void setInputOctets(Long inputOctets) { this.inputOctets = inputOctets; }

    public Long getOutputOctets() { return outputOctets; }
    public void setOutputOctets(Long outputOctets) { this.outputOctets = outputOctets; }

    public Long getSessionTime() { return sessionTime; }
    public void setSessionTime(Long sessionTime) { this.sessionTime = sessionTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(Instant lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}

