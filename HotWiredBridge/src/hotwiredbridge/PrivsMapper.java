package hotwiredbridge;

import hotwiredbridge.hotline.HotlinePrivileges;
import wired.event.AccountPrivileges;

public class PrivsMapper {
	public static AccountPrivileges convertHotlineToWiredPrivs(HotlinePrivileges privs) {
		AccountPrivileges wiredPrivs = new AccountPrivileges();
		wiredPrivs.setCanGetUserInfo(privs.hasPrivilege(HotlinePrivileges.CAN_GET_USER_INFO));
		wiredPrivs.setCanBroadcast(privs.hasPrivilege(HotlinePrivileges.CAN_POST_NEWS));
		wiredPrivs.setCanPostNews(privs.hasPrivilege(HotlinePrivileges.CAN_POST_NEWS));
		wiredPrivs.setCanClearNews(privs.hasPrivilege(HotlinePrivileges.CAN_POST_NEWS));
		wiredPrivs.setCanDownload(privs.hasPrivilege(HotlinePrivileges.CAN_DOWNLOAD_FILES));
		wiredPrivs.setCanUpload(privs.hasPrivilege(HotlinePrivileges.CAN_UPLOAD_FILES));
		wiredPrivs.setCanUploadAnywhere(privs.hasPrivilege(HotlinePrivileges.CAN_UPLOAD_ANYWHERE));
		wiredPrivs.setCanCreateFolders(privs.hasPrivilege(HotlinePrivileges.CAN_CREATE_FOLDERS));
		wiredPrivs.setCanAlterFiles(privs.hasPrivilege(HotlinePrivileges.CAN_RENAME_FILES));
		wiredPrivs.setCanDeleteFiles(privs.hasPrivilege(HotlinePrivileges.CAN_DELETE_FILES));
		wiredPrivs.setCanViewDropBoxes(privs.hasPrivilege(HotlinePrivileges.CAN_VIEW_DROP_BOXES));
		wiredPrivs.setCanCreateAccounts(privs.hasPrivilege(HotlinePrivileges.CAN_CREATE_USERS));
		wiredPrivs.setCanEditAccounts(privs.hasPrivilege(HotlinePrivileges.CAN_MODIFY_USERS));
		wiredPrivs.setCanDeleteAccounts(privs.hasPrivilege(HotlinePrivileges.CAN_DELETE_USERS));
		wiredPrivs.setCanKickUsers(privs.hasPrivilege(HotlinePrivileges.CAN_DISCONNECT_USERS));
		wiredPrivs.setCanBanUsers(privs.hasPrivilege(HotlinePrivileges.CAN_DELETE_USERS));
		wiredPrivs.setCanNotBeKicked(privs.hasPrivilege(HotlinePrivileges.CANNOT_BE_DISCONNECTED));
		wiredPrivs.setCanChangeTopic(privs.hasPrivilege(HotlinePrivileges.CAN_POST_NEWS));
		wiredPrivs.setCanBroadcast(privs.hasPrivilege(HotlinePrivileges.CAN_BROADCAST));
		return wiredPrivs;
	}

	public static byte[] convertWiredPrivsToHotlinePrivs(AccountPrivileges wiredPrivs) {
		HotlinePrivileges privs = new HotlinePrivileges();
		privs.setPrivilege(HotlinePrivileges.CAN_GET_USER_INFO, wiredPrivs.getCanGetUserInfo());
		privs.setPrivilege(HotlinePrivileges.CAN_UPLOAD_ANYWHERE, wiredPrivs.getCanUploadAnywhere());
		privs.setPrivilege(HotlinePrivileges.CAN_USE_ANY_NAME, true);
		privs.setPrivilege(HotlinePrivileges.DONT_SHOW_AGREEMENT, false);
		privs.setPrivilege(HotlinePrivileges.CAN_COMMENT_FILES, wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_COMMENT_FOLDERS, wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_VIEW_DROP_BOXES, wiredPrivs.getCanViewDropBoxes());
		privs.setPrivilege(HotlinePrivileges.CAN_MAKE_ALIASES, wiredPrivs.getCanUploadAnywhere());
		privs.setPrivilege(HotlinePrivileges.CAN_READ_USERS, wiredPrivs.getCanEditAccounts());
		privs.setPrivilege(HotlinePrivileges.CAN_MODIFY_USERS, wiredPrivs.getCanEditAccounts());
		privs.setPrivilege(HotlinePrivileges.CAN_READ_NEWS, true);
		privs.setPrivilege(HotlinePrivileges.CAN_POST_NEWS, wiredPrivs.getCanPostNews());
		privs.setPrivilege(HotlinePrivileges.CAN_DISCONNECT_USERS, wiredPrivs.getCanKickUsers());
		privs.setPrivilege(HotlinePrivileges.CANNOT_BE_DISCONNECTED, wiredPrivs.getCanNotBeKicked());
		privs.setPrivilege(HotlinePrivileges.CAN_MOVE_FOLDERS, wiredPrivs.getCanCreateFolders() && wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_READ_CHAT, true);
		privs.setPrivilege(HotlinePrivileges.CAN_SEND_CHAT, true);
		privs.setPrivilege(HotlinePrivileges.CAN_START_CHATS, true);
		privs.setPrivilege(HotlinePrivileges.CAN_CREATE_USERS, wiredPrivs.getCanCreateAccounts());
		privs.setPrivilege(HotlinePrivileges.CAN_DELETE_USERS, wiredPrivs.getCanDeleteAccounts());
		privs.setPrivilege(HotlinePrivileges.CAN_DELETE_FILES, wiredPrivs.getCanDeleteFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_UPLOAD_FILES, wiredPrivs.getCanUpload());
		privs.setPrivilege(HotlinePrivileges.CAN_DOWNLOAD_FILES, wiredPrivs.getCanDownload());
		privs.setPrivilege(HotlinePrivileges.CAN_RENAME_FILES, wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_MOVE_FILES, wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_CREATE_FOLDERS, wiredPrivs.getCanCreateFolders());
		privs.setPrivilege(HotlinePrivileges.CAN_DELETE_FOLDERS, wiredPrivs.getCanCreateFolders() && wiredPrivs.getCanDeleteFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_RENAME_FOLDERS, wiredPrivs.getCanCreateFolders() && wiredPrivs.getCanAlterFiles());
		privs.setPrivilege(HotlinePrivileges.CAN_BROADCAST, wiredPrivs.getCanBroadcast());
		return privs.toByteArray();
	}	
}
