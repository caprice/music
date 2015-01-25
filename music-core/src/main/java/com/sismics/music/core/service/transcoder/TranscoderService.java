package com.sismics.music.core.service.transcoder;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.io.Files;
import com.sismics.music.core.dao.dbi.TranscoderDao;
import com.sismics.music.core.model.dbi.Track;
import com.sismics.music.core.model.dbi.Transcoder;
import com.sismics.util.io.TranscodedInputStream;

/**
 * Transcoder service.
 *
 * @author jtremeaux
 */
public class TranscoderService {
    /**
     * Returns a transcoded stream for a track.
     *
     * @param track Track to transcode
     * @param seek Time to seek (in seconds)
     * @return Transcoded input stream
     * @throws Exception
     */
    public InputStream getTranscodedInputStream(Track track, int seek, Transcoder transcoder) throws Exception {
        ProcessBuilder pb = getProcessBuilder(track, seek, transcoder);
        return new TranscodedInputStream(pb);
    }

    private ProcessBuilder getProcessBuilder(Track track, int seek, Transcoder transcoder) {
        List<String> result = new LinkedList<String>(Arrays.asList(StringUtils.split(transcoder.getStep1())));

        int maxBitRate = 128; // TODO Configure the bitrate somewhere
        for (int i = 1; i < result.size(); i++) {
            String cmd = result.get(i);
            if (cmd.contains("%b")) {
                cmd = cmd.replace("%b", String.valueOf(maxBitRate));
            }
            if (cmd.contains("%ss")) {
                cmd = cmd.replace("%ss", String.valueOf(seek));
            }
            if (cmd.contains("%s")) {
                cmd = cmd.replace("%s", track.getFileName());
            }

            result.set(i, cmd);
        }
        return new ProcessBuilder(result);
    }

    /**
     * Return a suitable transcoder for a track.
     * 
     * @param track Track
     * @return Transcoder
     */
    public Transcoder getSuitableTranscoder(Track track) {
        TranscoderDao transcoderDao = new TranscoderDao();
        String fileExtension = Files.getFileExtension(track.getFileName());
        for (Transcoder transcoder : transcoderDao.findAll()) {
            String source = " " + transcoder.getSource() + " ";
            if (source.contains(" " + fileExtension + " ")) {
                return transcoder;
            }
        }
        return null;
    }
}
