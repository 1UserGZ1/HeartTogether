package com.dyxiaojiazi.hearttogether.data;

public class SharedHealthData {
    private float currentHealth;
    private float maxHealth;
    private float absorption;

    public SharedHealthData(float currentHealth, float maxHealth, float absorption) {
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.absorption = absorption;
    }

    public float getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(float currentHealth) { this.currentHealth = currentHealth; }

    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }

    public float getAbsorption() { return absorption; }
    public void setAbsorption(float absorption) { this.absorption = absorption; }
}