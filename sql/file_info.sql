/*
 Navicat Premium Dump SQL

 Source Server         : docker_mysql_8_1
 Source Server Type    : MySQL
 Source Server Version : 80404 (8.4.4)
 Source Host           : 127.0.0.1:3307
 Source Schema         : file_info

 Target Server Type    : MySQL
 Target Server Version : 80404 (8.4.4)
 File Encoding         : 65001

 Date: 19/05/2025 13:50:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_meta_data
-- ----------------------------
DROP TABLE IF EXISTS `t_meta_data`;
CREATE TABLE `t_meta_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name_prefix` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `hash` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_subfix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `add_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `size` bigint NOT NULL DEFAULT '0',
  `add_user` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `add_host` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fixed_name_prefix` (`hash`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=latin1;

SET FOREIGN_KEY_CHECKS = 1;
