package groupbee.attendance.controller;

import groupbee.attendance.dto.AttendanceDto;
import groupbee.attendance.service.attendance.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @Operation(
            summary = "로그인 아이디 별 근태 리스트",
            description = "로그인 아이디 별 근태으로 근태리스트을 반환"
    )
    @GetMapping("list")
    public ResponseEntity<List<AttendanceDto>> getAttendanceList() {
        return attendanceService.getAttendanceList();
    }
    @Operation(
            summary = "출근",
            description = "{\n" +
                    "    \"checkIn\" : \"localDateTime\"\n" +
                    "}"
    )
    @PostMapping("checkin")
    public ResponseEntity<String> checkIn(@RequestBody AttendanceDto attendanceDto) {
        return attendanceService.checkIn(attendanceDto);
    }

    @Operation(
            summary = "퇴근",
            description = "{\\n\" +\n" +
                    "                    \"    \\\"checkOut\\\" : \\\"localDateTime\\\"\\n\" +\n" +
                    "                    \"}"
    )
    @PostMapping("checkout")
    public ResponseEntity<String> checkOut(@RequestBody AttendanceDto attendanceDto) {
        return attendanceService.checkOut(attendanceDto);
    }

    @GetMapping("/todayCheckIn")
    public ResponseEntity<?> getTodayCheckIn() {
        return attendanceService.getTodayCheckIn();
    }
}
