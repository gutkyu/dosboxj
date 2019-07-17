package org.gutkyu.dosboxj.misc;


public final class CrossModule {
    // 함수 open_directory,read_directory_first,read_directory_next,close_directory는
    // 호스트의 디렉토리 경로를 살피는데 사용, java의 file io관련 라이브러리로 대체, 이 함수들은 사용하지 않는다.

    // public static System.IO.DirectoryInfo open_directory(byte[] dirname) {
    // if (dirname == null) return null;

    // int len = CString.strlen(dirname);
    // if (len == 0) return null;

    // safe_strncpy(dir.base_path, dirname, MAX_PATH);

    // if (dirname[len - 1] == '\\') strcat(dir.base_path, "*.*");
    // else strcat(dir.base_path, "\\*.*");

    // System.IO.DirectoryInfo dir = new System.IO.DirectoryInfo(Encoding.ASCII.GetString(dirname));
    // dir.

    // dir.handle = INVALID_HANDLE_VALUE;

    // return (access(dirname,0) ? NULL : &dir);
    // }
}
