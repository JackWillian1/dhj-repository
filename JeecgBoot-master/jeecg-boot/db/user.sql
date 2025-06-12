/*
 Navicat Premium Dump SQL

 Source Server         : MyDemoDB
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44-log)
 Source Host           : localhost:3306
 Source Schema         : my_demo_db

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44-log)
 File Encoding         : 65001

 Date: 26/05/2025 14:22:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `record_login_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `fail_login_count` int(11) NOT NULL,
  `account_lock` tinyint(1) NOT NULL,
  `current_login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `success_login_count` int(11) NOT NULL,
  `force_attacks_count` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`record_login_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_bin ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
