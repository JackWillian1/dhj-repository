package org.jeecg.log.dao;

import org.jeecg.log.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;

//<UserLog, Long> UserLog 表示要操作的实体类，Long 表示实体类的主键类型。
public interface UserLogRepository extends JpaRepository<UserLog, Long> {
}
