-- 重置 image CRMEB Java 20211027 有问题论坛留言
-- 清空附件表
DROP TABLE IF EXISTS `eb_system_attachment`;

CREATE TABLE `eb_system_attachment` (
  `att_id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '附件名称',
  `att_dir` varchar(200) NOT NULL DEFAULT '' COMMENT '附件路径',
  `satt_dir` varchar(200) DEFAULT NULL COMMENT '压缩图片路径',
  `att_size` char(30) NOT NULL DEFAULT '' COMMENT '附件大小',
  `att_type` char(30) NOT NULL DEFAULT '' COMMENT '附件类型',
  `pid` int(10) NOT NULL DEFAULT '0' COMMENT '分类ID0编辑器,1商品图片,2拼团图片,3砍价图片,4秒杀图片,5文章图片,6组合数据图， 7前台用户',
  `image_type` tinyint(1) unsigned NOT NULL DEFAULT '1' COMMENT '图片上传类型 1本地 2七牛云 3OSS 4COS ',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`att_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='附件管理表';

LOCK TABLES `eb_system_attachment` WRITE;
/*!40000 ALTER TABLE `eb_system_attachment` DISABLE KEYS */;

INSERT INTO `eb_system_attachment` (`att_id`, `name`, `att_dir`, `satt_dir`, `att_size`, `att_type`, `pid`, `image_type`, `create_time`, `update_time`)
VALUES
	(713,'登录界面java_.png','','crmebimage/public/maintain/2021/10/27/6a9eaa01b463444f9d8eeaeef7d3f80b0f0p0wdge4.png','117604','png',669,1,'2021-10-27 17:07:11','2021-10-27 17:07:11'),
	(714,'crmebLogo.png','','crmebimage/public/maintain/2021/10/27/f31b7885269b488a926042c07daa923eyfns7dl3ju.png','24056','png',669,1,'2021-10-27 17:07:11','2021-10-27 17:07:11'),
	(715,'黄铜会员.png','','crmebimage/public/maintain/2021/10/27/be855bcb9aeb40a784b10d0545100f4433df2y06rs.png','4248','png',670,1,'2021-10-27 17:07:28','2021-10-27 17:07:28'),
	(716,'黄金会员.png','','crmebimage/public/maintain/2021/10/27/902b3d986c3c4e649c9e3203b5760ece06vrp0wsk6.png','4699','png',670,1,'2021-10-27 17:07:28','2021-10-27 17:07:28'),
	(717,'钻石会员.png','','crmebimage/public/maintain/2021/10/27/60bfd033867a4297b7c70513343b54c0vjv12514dm.png','5110','png',670,1,'2021-10-27 17:07:28','2021-10-27 17:07:28'),
	(718,'白银会员.png','','crmebimage/public/maintain/2021/10/27/f392f6213d264de0b1b8d978cc9ea42fs7ixr2secp.png','3894','png',670,1,'2021-10-27 17:07:28','2021-10-27 17:07:28'),
	(719,'普通会员.png','','crmebimage/public/maintain/2021/10/27/fbd3291f86db435da31b15d9878f1dd8xfvzmsouja.png','3629','png',670,1,'2021-10-27 17:07:28','2021-10-27 17:07:28'),
	(720,'05682990b4616ca80c646e4f97f4beca.png','','crmebimage/public/maintain/2021/10/27/d59840f6c2e3407a99f7444ebeea342bjw8j14wwpm.png','51739','png',671,1,'2021-10-27 17:08:04','2021-10-27 17:08:04'),
	(721,'9f55a427c2adcf90a6ea98c37c181b07.png','','crmebimage/public/maintain/2021/10/27/5c5a956147ea48248ef668c66b4f5ab9al8a2fk09l.png','44675','png',671,1,'2021-10-27 17:08:04','2021-10-27 17:08:04'),
	(722,'efbcede378fd71508dcc3e2f773bcb2d.png','','crmebimage/public/maintain/2021/10/27/e39b08a7397c44349437bd25b0c7cde0fyafqiwizz.png','52073','png',671,1,'2021-10-27 17:08:04','2021-10-27 17:08:04'),
	(723,'fa77fd863f04fcd2d516ea1fa380b8dd.png','','crmebimage/public/maintain/2021/10/27/a82d2ecfcd4b44f4bc099489ae298b4cfkkazi887z.png','49736','png',671,1,'2021-10-27 17:08:04','2021-10-27 17:08:04'),
	(724,'e33b6334224002814a65209ce5465a0b.png','','crmebimage/public/maintain/2021/10/27/d251a72a49814bdab7cfcd65a43756e2u110fkpefl.png','50109','png',671,1,'2021-10-27 17:08:04','2021-10-27 17:08:04'),
	(725,'WechatIMG124.jpg','','crmebimage/public/maintain/2021/10/27/0792ec70ed094d939c8f6b3b0527f285du6gr7oa82.jpg','73725','jpeg',667,1,'2021-10-27 17:08:32','2021-10-27 17:08:32'),
	(726,'WechatIMG126.jpg','','crmebimage/public/maintain/2021/10/27/c2a5c64a9f3f4541be012a71d91c15cefpcx16fe9d.jpg','80741','jpeg',667,1,'2021-10-27 17:08:32','2021-10-27 17:08:32'),
	(727,'3.png','','crmebimage/public/maintain/2021/10/27/6f79b77c9e5c41d7a83ecf15b51e6e3fkdkasuzbhy.png','2081','png',672,1,'2021-10-27 17:09:09','2021-10-27 17:09:09'),
	(728,'2.png','','crmebimage/public/maintain/2021/10/27/7b6404d1f11745a3b9b2b38ffa5bf99drflg1qmeej.png','9371','png',672,1,'2021-10-27 17:09:09','2021-10-27 17:09:09'),
	(729,'LOGO@2x.png','','crmebimage/public/maintain/2021/10/27/40bd78716bd3499c85221cde11b67aebxddavqnf0e.png','9022','png',672,1,'2021-10-27 17:09:09','2021-10-27 17:09:09'),
	(730,'1.png','','crmebimage/public/maintain/2021/10/27/01e533f98776427385f7feb171d19ec6d0hmdutmyf.png','2932','png',672,1,'2021-10-27 17:09:09','2021-10-27 17:09:09'),
	(731,'抽奖活动.png','','crmebimage/public/maintain/2021/10/27/be39b001676b41b58110e08703cdc924x1hvzqpk30.png','6819','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(732,'我的收藏.png','','crmebimage/public/maintain/2021/10/27/d339f629c7c74c13a8f545419b298eb8p2643jma8j.png','9382','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(733,'我要签到.png','','crmebimage/public/maintain/2021/10/27/616d82ceed7546c68fa5ea517b1376babo79jaq71i.png','7932','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(734,'地址管理.png','','crmebimage/public/maintain/2021/10/27/e5c38157d7014a2b91efd296b821cd063d7sa44jms.png','8846','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(735,'砍价活动.png','','crmebimage/public/maintain/2021/10/27/8e320a35bc79452da1db3098e14f9b01jrn9990jmo.png','8954','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(736,'行业资讯.png','','crmebimage/public/maintain/2021/10/27/e26d58b2f8ad49f0823faa356e556682dwoizpos08.png','7551','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(737,'秒杀活动.png','','crmebimage/public/maintain/2021/10/27/1e8034655e154a0ca6e5ad41d2fbd690bt5rfqd88z.png','8883','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(738,'订单管理.png','','crmebimage/public/maintain/2021/10/27/f91b821e320541b58d11f44ae2105e48pfm2qugj3b.png','7640','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(739,'领优惠券.png','','crmebimage/public/maintain/2021/10/27/f36787d6984242bbae0d55adb0cd8a0dgq8608bh22.png','8748','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(740,'拼团活动.png','','crmebimage/public/maintain/2021/10/27/d5d9da38533c4f26ad424ce7e7e9811cyh6bgksx7l.png','10477','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(741,'商品分类.png','','crmebimage/public/maintain/2021/10/27/23ba5fcedadb4bbc9fcbc66f639bee15lq2tcpabt5.png','8292','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(742,'积分商城.png','','crmebimage/public/maintain/2021/10/27/3ee0cec4c36847cb81698356663608d03y9qvn3fet.png','7454','png',666,1,'2021-10-27 17:09:43','2021-10-27 17:09:43'),
	(743,'690x280.jpg','','crmebimage/public/maintain/2021/10/27/83a02eef7deb4d0294253afbfc25f9a32f3p4mi9wf.jpg','56701','jpeg',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(744,'690x290.jpg','','crmebimage/public/maintain/2021/10/27/00c86ad2a9d441758a69533988a9fe01eausvtq2gf.jpg','60436','jpeg',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(745,'banner1.png','','crmebimage/public/maintain/2021/10/27/952e9086d6744c9a91633d6825e23cdc1bifcgf4lt.png','268610','png',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(746,'banner-2.png','','crmebimage/public/maintain/2021/10/27/8cdc86cf541d4d48b304804a755c12377j19ds03hh.png','427586','png',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(747,'banner-1.png','','crmebimage/public/maintain/2021/10/27/13d3aceafe7e4fe0abd4f11837287911rhxgw0un7u.png','363824','png',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(748,'banner2.png','','crmebimage/public/maintain/2021/10/27/fed23e9165e2481384d848a8858d5733p5byp80ew8.png','364279','png',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(749,'banner.png','','crmebimage/public/maintain/2021/10/27/7a8daeafd6d142c09ce068424071d1e95bdc26t8sv.png','342021','png',673,1,'2021-10-27 17:10:52','2021-10-27 17:10:52'),
	(750,'2.jpg','','crmebimage/public/maintain/2021/10/27/d4206c8046c249cb8beffef0da4fffae6gp1g68dk5.jpg','19973','jpeg',674,1,'2021-10-27 17:11:08','2021-10-27 17:11:08'),
	(751,'1.jpg','','crmebimage/public/maintain/2021/10/27/5271f4e418264c029bb380fb5f74592ahx1avlj257.jpg','20103','jpeg',674,1,'2021-10-27 17:11:08','2021-10-27 17:11:08'),
	(752,'3.jpg','','crmebimage/public/maintain/2021/10/27/c50aaface755459893178e9359decb67km5hwj4njx.jpg','26376','jpeg',674,1,'2021-10-27 17:11:08','2021-10-27 17:11:08'),
	(753,'会员中心.png','','crmebimage/public/maintain/2021/10/27/dbf0854cf16c45e1b33cf42a1582c35dgij3vmgtoo.png','2308','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(754,'发票.png','','crmebimage/public/maintain/2021/10/27/be205fa95eea4f109a7902afd371e3c3wv84rrihua.png','1632','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(755,'我的等级.png','','crmebimage/public/maintain/2021/10/27/9b1ffc4d42454477945968ca7e5dd62aj7bvekf5hj.png','2330','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(756,'隐私协议.png','','crmebimage/public/maintain/2021/10/27/304497027ae84bbc9cf83dd769e0ec37qt738tfz5b.png','1673','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(757,'积分兑换.png','','crmebimage/public/maintain/2021/10/27/751911d7a13447a7b169ddd4c0522e6fhl8kjarrg7.png','1354','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(758,'砍价记录.png','','crmebimage/public/maintain/2021/10/27/23fcec88788147219813d7a13707790dwe7u42ja0w.png','1811','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(759,'我的收藏.png','','crmebimage/public/maintain/2021/10/27/15006b0b16964168ac3f5974d6bf7cfe3rp3wkz0b1.png','1343','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(760,'优惠券.png','','crmebimage/public/maintain/2021/10/27/94dfdd38e1a04bf6aee9bfeb32bed0d0wdvmgs4pxp.png','1859','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(761,'中奖记录.png','','crmebimage/public/maintain/2021/10/27/62c14407330241aaa0a3bce211fea163d4s5h7gqz4.png','1701','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(762,'我的推广.png','','crmebimage/public/maintain/2021/10/27/b7e139d984e74058848ed6c5626f9c20ex29y3tgp3.png','2262','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(763,'客服接待.png','','crmebimage/public/maintain/2021/10/27/2cf2ec3766954e52993d76f54ce8fc1436eaiplf5v.png','2037','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(764,'联系客服.png','','crmebimage/public/maintain/2021/10/27/9e2a4c0edb6e4e40ad86d28204ece1d26yn8s9gr8c.png','1979','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(765,'签到.png','','crmebimage/public/maintain/2021/10/27/fb3b590b62a04f5a8740700d544a8843kh4ibld1du.png','2237','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(766,'统计管理.png','','crmebimage/public/maintain/2021/10/27/f2334cc1fa5341e6b190114f2f26b5c5juw5wdtto1.png','1941','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(767,'积分中心.png','','crmebimage/public/maintain/2021/10/27/907511df529047beb9dd5ef52e3ae3ecaq39uchgo1.png','2239','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(768,'地址管理.png','','crmebimage/public/maintain/2021/10/27/f5e7490f8591414eb001b3fc31f9788codzp58paj5.png','2053','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(769,'我的余额.png','','crmebimage/public/maintain/2021/10/27/d3e1948e1f5741e88f940f9ae7f1f1d69wz115sqlj.png','2118','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(770,'订单核销.png','','crmebimage/public/maintain/2021/10/27/1557f23e8cbf4ac3adf8d0ae46f698e9q8puf6j1ps.png','2055','png',675,1,'2021-10-27 17:20:20','2021-10-27 17:20:20'),
	(771,'crmebLogo.png','','crmebimage/public/operation/2021/10/27/75773959c66448debfc27384a4ed82e6t461ds0b71.png','24056','png',672,1,'2021-10-27 17:23:04','2021-10-27 17:23:04');

/*!40000 ALTER TABLE `eb_system_attachment` ENABLE KEYS */;
UNLOCK TABLES;





-- eb_system_config 更新设置中的log等
update eb_system_config c set c.value = "crmebimage/operation/2021/02/25/1044da7cb6544c978f9c95fde324cbb4zxg8iby21b.png" where c.id=6076; 
update eb_system_config c set c.value = "crmebimage/operation/2021/02/25/bf74edf7106544f1a7893a2796ce4260uaeyzinl3h.png" where c.id=6077; 
update eb_system_config c set c.value = "crmebimage/operation/2021/02/25/7aa0a3fe4b26440f8c10a5a27ec55502u39xni4dsr.png" where c.id=6080; 
update eb_system_config c set c.value = "crmebimage/public/maintain/2021/10/27/40bd78716bd3499c85221cde11b67aebxddavqnf0e.png" where c.id=7139; 
update eb_system_config c set c.value = "crmebimage/public/operation/2021/10/27/75773959c66448debfc27384a4ed82e6t461ds0b71.png" where c.id=7140; 
update eb_system_config c set c.value = "crmebimage/public/maintain/2021/10/27/40bd78716bd3499c85221cde11b67aebxddavqnf0e.png" where c.id=7153; 
update eb_system_config c set c.value = "crmebimage/public/operation/2021/10/27/75773959c66448debfc27384a4ed82e6t461ds0b71.png" where c.id=7154; 
update eb_system_config c set c.value = "crmebimage/operation/2021/07/16/ca7f639e3bbf4151bfd81cf72d6f9e1ace2t4yhl74.png" where c.id=7155; 
update eb_system_config c set c.value = "crmebimage/operation/2021/07/16/581b259c5bed44f68e264b09d66cda97u6oztndt6s.jpg" where c.id=7156; 

-- 重置附件分类 如果自己添加过分类这里的id需要自己维护下
--delete from table eb_category c where c.type = 2; 可以手动清除原有素材分类
-- 插入新的分类数据
INSERT INTO `eb_category` (`id`, `pid`, `path`, `name`, `type`, `url`, `extra`, `status`, `sort`, `create_time`, `update_time`)
VALUES
(665,0,'/0/','移动端',2,'url',NULL,0,1,'2021-10-27 17:05:02','2021-10-27 17:05:02'),
	(666,665,'/0/665/','金刚区',2,'url',NULL,0,1,'2021-10-27 17:05:09','2021-10-27 17:05:09'),
	(667,665,'/0/665/','分享海报',2,'url',NULL,0,1,'2021-10-27 17:05:29','2021-10-27 17:05:29'),
	(668,665,'/0/665/','banner',2,'url',NULL,0,1,'2021-10-27 17:05:47','2021-10-27 17:05:47'),
	(669,0,'/0/','管理端',2,'url',NULL,0,1,'2021-10-27 17:06:01','2021-10-27 17:06:01'),
	(670,669,'/0/669/','会员等级',2,'url',NULL,0,1,'2021-10-27 17:06:21','2021-10-27 17:06:21'),
	(671,669,'/0/669/','分销等级',2,'url',NULL,0,1,'2021-10-27 17:07:51','2021-10-27 17:07:51'),
	(672,665,'/0/665/','logo',2,'url',NULL,0,1,'2021-10-27 17:08:53','2021-10-27 17:08:53'),
	(673,668,'/0/665/668/','首页',2,'url',NULL,0,1,'2021-10-27 17:10:29','2021-10-27 17:10:29'),
	(674,668,'/0/665/668/','页中',2,'url',NULL,0,1,'2021-10-27 17:10:41','2021-10-27 17:10:41'),
	(675,665,'/0/665/','我的服务',2,'url',NULL,0,1,'2021-10-27 17:20:05','2021-10-27 17:20:05');