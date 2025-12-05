package com.csg.airtel.aaa4j.domain.model;

import java.time.Instant;

/**
 * Represents a user session in the RADIUS server.
 * <p>
 * Thread-safety note: Mutable fields are marked volatile to ensure visibility
 * across threads if instances are shared (e.g., cached sessions). For compound
 * operations involving multiple fields, external synchronization may be required.
 * </p>
 */
public class UserSession {
    private volatile Long id;
    private volatile String sessionId;
    private volatile String username;
    private volatile String nasIP;
    private volatile String nasPortId;
    private volatile String framedIPAddress;
    private volatile Long inputOctets;
    private volatile Long outputOctets;
    private volatile Long sessionTime;
    private volatile String status; // ACTIVE, STOPPED
    private volatile Instant startTime;
    private volatile Instant lastUpdateTime;
    private volatile Instant endTime;

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

