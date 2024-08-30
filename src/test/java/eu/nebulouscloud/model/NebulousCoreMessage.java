package eu.nebulouscloud.model;

import java.util.Date;
import java.util.Map;

public class NebulousCoreMessage {
    private Date receptionDate;
    private String topic;
    private Map<String,Object> payload;
    private String applicationId;
    private String correlationId;

    public NebulousCoreMessage(Date receptionDate, String topic, Map<String, Object> payload, String applicationId, String correlationId) {
        this.receptionDate = receptionDate;
        this.topic = topic != null ? (topic.startsWith("topic://") ? topic : "topic://" + topic) : null;
        this.payload = payload;
        this.applicationId = applicationId;
        this.correlationId = correlationId;
    }

    public NebulousCoreMessage() {
    }

    public NebulousCoreMessage(Map<String, Object> payload, String topic) {
        this.topic = topic != null ? (topic.startsWith("topic://") ? topic : "topic://" + topic) : null;
        this.payload = payload;
    }

    public Date getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(Date receptionDate) {
        this.receptionDate = receptionDate;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
