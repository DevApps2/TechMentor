package com.techmentor.api.dto;

/** O formato aceito para o link do vídeo de uma aula. Só confere o formato; não consulta o YouTube. */
public final class LinksDeVideo {
    private LinksDeVideo() { }

    private static final String ID_DO_YOUTUBE = "[A-Za-z0-9_-]{11}";

    /** YouTube (watch?v=ID ou youtu.be/ID, com o id de 11 caracteres) ou um arquivo .mp4 em https. */
    public static final String PADRAO =
            "^(https://(www\\.|m\\.)?youtube\\.com/watch\\?v=" + ID_DO_YOUTUBE + "([&#]\\S*)?"
                    + "|https://youtu\\.be/" + ID_DO_YOUTUBE + "(\\?\\S*)?"
                    + "|https://\\S+\\.mp4(\\?\\S*)?)$";

    public static final String MENSAGEM =
            "O link do vídeo deve ser do YouTube (https://www.youtube.com/watch?v=...) ou um arquivo .mp4 em https.";
}
