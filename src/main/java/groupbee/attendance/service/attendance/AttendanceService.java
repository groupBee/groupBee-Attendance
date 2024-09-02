package groupbee.attendance.service.attendance;

import feign.FeignException;
import groupbee.attendance.dto.AttendanceDto;
import groupbee.attendance.service.feign.FeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final FeignClient feignClient;
    private final String odooUrl = System.getenv("ODOO_URL");
    private final String odooDb = System.getenv("ODOO_DB");
    private final int odooUid = Integer.parseInt(System.getenv("ODOO_UID"));
    private final String odooPassword = System.getenv("ODOO_PASSWORD");

    public ResponseEntity<List<AttendanceDto>> getAttendanceList() {
        try {
            Map<String, Object> employeeInfo = feignClient.getHrInfo();
            int hrId = (int) employeeInfo.get("id");

            XmlRpcClient models = new XmlRpcClient();
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", odooUrl)));
            models.setConfig(config);

            // 출퇴근 기록을 검색합니다.
            Object[] attendanceRecords = (Object[]) models.execute(
                    "execute_kw", Arrays.asList(
                            odooDb, odooUid, odooPassword,
                            "hr.attendance", "search_read",
                            List.of(
                                    List.of(
                                            Arrays.asList("employee_id", "=", hrId) // 직원 ID
                                    )
                            ),
                            Map.of("fields", Arrays.asList("check_in", "check_out", "worked_hours"))
                    )
            );

            log.info("attendanceRecords: {}", attendanceRecords);

            // Object[]를 AttendanceDto 리스트로 변환
            List<AttendanceDto> attendanceList = new ArrayList<>();
            for (Object record : attendanceRecords) {
                Map<String, Object> attendanceMap = (Map<String, Object>) record;
                AttendanceDto dto = new AttendanceDto();
                dto.setId((int) attendanceMap.get("id"));
                dto.setEmployeeId(hrId);

                Object checkIn = attendanceMap.get("check_in");
                if (checkIn instanceof String) {
                    dto.setCheckIn(Timestamp.valueOf((String) attendanceMap.get("check_in")));
                } else {
                    dto.setCheckIn(null);
                }

                Object checkOut = attendanceMap.get("check_out");
                if (checkOut instanceof String) {
                    dto.setCheckOut(Timestamp.valueOf((String) attendanceMap.get("check_out")));
                } else {
                    dto.setCheckOut(null);
                }

                Object workedHours = attendanceMap.get("worked_hours");
                if (workedHours != null) {
                    dto.setWorkHours((double) workedHours);
                } else {
                    dto.setWorkHours(0.0); // 또는 적절한 기본값으로 설정
                }
                attendanceList.add(dto);
            }

            return ResponseEntity.status(HttpStatus.OK).body(attendanceList);
        } catch (FeignException.BadRequest e) {
            // 400 Bad Request 발생 시 처리
            System.out.println("Bad Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (FeignException e) {
            // 기타 FeignException 발생 시 처리
            System.out.println("Feign Exception: " + e.getMessage());
            return ResponseEntity.status(e.status()).body(null);
        } catch (Exception e) {
            // 일반 예외 처리
            System.out.println("Exception: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<String> checkIn(AttendanceDto attendanceDto) {
        try{
            Map<String, Object> employeeInfo = feignClient.getHrInfo();
            int hrId = (int) employeeInfo.get("id");

            XmlRpcClient models = new XmlRpcClient();
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", odooUrl)));
            models.setConfig(config);

            // 1. 현재 로컬 시스템의 LocalDateTime 가져오기
            String utcCheckInTime = attendanceDto.getCheckIn().toLocalDateTime()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

//            String utcCheckInTime = attendanceDto.getCheckIn().atZone(ZoneId.of("Asia/Seoul"))
//                    .withZoneSameInstant(ZoneId.of("UTC"))
//                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//            String checkInTime = attendanceDto.getCheckIn().toLocalDateTime()
//                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.println(attendanceDto.getCheckIn());
            System.out.println(utcCheckInTime);

            // 출근 데이터를 Map으로 정의
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("employee_id", hrId); // 직원 ID
            attendanceData.put("check_in", utcCheckInTime); // 출근 시간

            // Odoo 에서 'hr.attendance' 모델을 사용하여 출근 데이터 생성
            int attendanceId = (int) models.execute("execute_kw", Arrays.asList(
                    odooDb, odooUid, odooPassword,
                    "hr.attendance", "create",
                    List.of(attendanceData)
            ));
            return ResponseEntity.status(HttpStatus.OK).body(String.valueOf(attendanceId));
//            return null;
        } catch (FeignException.BadRequest e) {
            // 400 Bad Request 발생 시 처리
            System.out.println("Bad Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (FeignException e) {
            // 기타 FeignException 발생 시 처리
            System.out.println("Feign Exception: " + e.getMessage());
            return ResponseEntity.status(e.status()).body(null);
        } catch (Exception e) {
            // 일반 예외 처리
            System.out.println("Exception: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<String> checkOut(AttendanceDto attendanceDto) {
        try {
            Map<String, Object> employeeInfo = feignClient.getHrInfo();
            int hrId = (int) employeeInfo.get("id");

            XmlRpcClient models = new XmlRpcClient();
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(String.format("%s/xmlrpc/2/object", odooUrl)));
            models.setConfig(config);

            LocalDateTime checkOutTime = attendanceDto.getCheckOut().toLocalDateTime();
            System.out.println(checkOutTime);

            String utcCheckOutTime = attendanceDto.getCheckOut().toLocalDateTime()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.println(utcCheckOutTime);

            // 현재 로컬 시간을 UTC로 변환하여 시작과 끝 시간 정의
            ZoneId systemZoneId = ZoneId.systemDefault();

            // 하루의 시작 시간을 가져와 UTC로 변환
            String formattedStartOfDayUtc = checkOutTime.toLocalDate().atStartOfDay().atZone(systemZoneId)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 하루의 끝 시간을 가져와 UTC로 변환
            String formattedEndOfDayUtc = checkOutTime.toLocalDate().atTime(23, 59, 59, 999999999).atZone(systemZoneId)
                    .withZoneSameInstant(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            System.out.println(formattedStartOfDayUtc);
            System.out.println(formattedEndOfDayUtc);

            // 퇴근 시간을 업데이트할 출근 기록 ID를 찾습니다.
            Object[] attendanceIds = (Object[]) models.execute("execute_kw", Arrays.asList(
                    odooDb, odooUid, odooPassword,
                    "hr.attendance", "search",
                    List.of(Arrays.asList(
                            Arrays.asList("employee_id", "=", hrId),  // 직원 ID
                            Arrays.asList("check_in", ">=", formattedStartOfDayUtc), // 특정 날짜의 출근 기록 검색
                            Arrays.asList("check_in", "<=", formattedEndOfDayUtc),
                            Arrays.asList("check_out", "=", false) // 아직 퇴근 기록이 없는 것만 검색
                    ))
            ));

            if (attendanceIds.length == 0) {
                System.out.println("출근 기록을 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body("출근 기록을 찾을 수 없습니다.");
            }

            int attendanceId = (int) attendanceIds[0]; // 검색된 첫 번째 ID 사용

            // 찾은 출근 기록에 퇴근 시간 기록
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("check_out", utcCheckOutTime); // 변환된 퇴근 시간 (UTC)

            models.execute("execute_kw", Arrays.asList(
                    odooDb, odooUid, odooPassword,
                    "hr.attendance", "write",
                    Arrays.asList(
                            List.of(attendanceId), // 업데이트할 기록 ID
                            updateData
                    )
            ));

            System.out.println("Attendance record updated with check-out time.");

            return null;
        } catch (FeignException.BadRequest e) {
            // 400 Bad Request 발생 시 처리
            System.out.println("Bad Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (FeignException e) {
            // 기타 FeignException 발생 시 처리
            System.out.println("Feign Exception: " + e.getMessage());
            return ResponseEntity.status(e.status()).body(null);
        } catch (Exception e) {
            // 일반 예외 처리
            System.out.println("Exception: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}

