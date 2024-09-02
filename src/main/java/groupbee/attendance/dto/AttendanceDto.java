package groupbee.attendance.dto;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceDto {
    private int id;
    private int employeeId;
    private int createUid;
    private int writeUid;
    private Timestamp checkIn;
    private Timestamp checkOut;
    private double workHours;
}
