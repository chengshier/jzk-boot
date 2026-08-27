-- 九州康 JZK V3.1 第二批菜单与 authority（MySQL 5.7，可重复执行）
SET @now=NOW();
SET @jk_root=(SELECT id FROM eb_system_menu WHERE component='/operation/jzk' AND is_delte=0 ORDER BY id LIMIT 1);

DROP PROCEDURE IF EXISTS jk_v31_menu;
DELIMITER $$
CREATE PROCEDURE jk_v31_menu(IN p_name varchar(64),IN p_component varchar(128),IN p_perm varchar(128),IN p_sort int)
BEGIN
  SET @mid=(SELECT id FROM eb_system_menu WHERE component=p_component OR perms=p_perm ORDER BY is_delte,id LIMIT 1);
  IF @mid IS NULL THEN
    INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
    VALUES(@jk_root,p_name,NULL,p_perm,p_component,'C',p_sort,1,0,@now,@now);
  ELSE
    UPDATE eb_system_menu SET pid=@jk_root,name=p_name,perms=p_perm,component=p_component,menu_type='C',sort=p_sort,is_show=1,is_delte=0,update_time=@now WHERE id=@mid;
  END IF;
END$$
DELIMITER ;

CALL jk_v31_menu('线下销售','/operation/jzk/offlineSale','admin:jk:offline:sale:list',61);
CALL jk_v31_menu('业绩明细','/operation/jzk/performanceRecord','admin:jk:performance:list',62);
CALL jk_v31_menu('经营收益','/operation/jzk/operationProfit','admin:jk:operation:profit:list',63);
DROP PROCEDURE jk_v31_menu;

SET @commission_menu=(SELECT id FROM eb_system_menu WHERE component='/operation/jzk/commissionRule' OR perms='admin:jk:commission:rule:list' ORDER BY is_delte,id LIMIT 1);

INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
SELECT @commission_menu,'佣金规则试算',NULL,'admin:jk:commission:rule:trial','','A',10,1,0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE perms='admin:jk:commission:rule:trial');
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
SELECT @commission_menu,'发布或停用佣金规则',NULL,'admin:jk:commission:rule:publish','','A',11,1,0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE perms='admin:jk:commission:rule:publish');

SET @offline_menu=(SELECT id FROM eb_system_menu WHERE perms='admin:jk:offline:sale:list' ORDER BY id LIMIT 1);
INSERT INTO eb_system_menu(pid,name,icon,perms,component,menu_type,sort,is_show,is_delte,create_time,update_time)
SELECT @offline_menu,'审核线下销售',NULL,'admin:jk:offline:sale:audit','','A',1,1,0,@now,@now
WHERE NOT EXISTS(SELECT 1 FROM eb_system_menu WHERE perms='admin:jk:offline:sale:audit');

SELECT id,pid,name,perms,component,menu_type FROM eb_system_menu
WHERE perms IN('admin:jk:offline:sale:list','admin:jk:offline:sale:audit','admin:jk:performance:list','admin:jk:operation:profit:list','admin:jk:commission:rule:trial','admin:jk:commission:rule:publish')
ORDER BY pid,id;
