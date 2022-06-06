package com.mykaarma.kcommunications.authorize;

import javax.servlet.http.HttpServletRequest;

public class IpUtils {

    public static String getIpAddress(HttpServletRequest request) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            String completeIp = ip;
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
                if (ip != null) {
                    completeIp += "," + ip;
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
                if (ip != null) {
                    completeIp += "," + ip;
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
                if (ip != null) {
                    completeIp += "," + ip;
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                if (ip != null) {
                    completeIp += "," + ip;
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if (ip != null) {
                    completeIp += "," + ip;
                }
            }
            return completeIp;
        } catch (Exception e) {
           
            return "Error";
        }
    }
 }
