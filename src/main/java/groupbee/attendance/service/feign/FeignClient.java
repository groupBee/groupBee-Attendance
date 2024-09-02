package groupbee.attendance.service.feign;

import groupbee.attendance.config.FeignConfig;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@org.springframework.cloud.openfeign.FeignClient(name = "employee", url = "${FEIGN_BASE_URL}", configuration = FeignConfig.class)
public interface FeignClient {
    @GetMapping("/api/employee/info")
    Map<String, Object> getEmployeeInfo();
}