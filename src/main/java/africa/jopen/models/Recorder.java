package africa.jopen.models;


import java.util.ArrayList;
import java.util.List;

import dev.onvoid.webrtc.media.MediaStreamTrack;
public class Recorder {
    final private List<TrackRecorder> trackRecorders = new ArrayList<>();

    Boolean recording = false;

    public Recorder() {

    }

    public Boolean getRecording() {
        return recording;
    }

    public void addTrack(String filename, MediaStreamTrack mediaStreamTrack) {
        TrackRecorder trackRecorder = new TrackRecorder(filename, mediaStreamTrack);
        trackRecorders.add(trackRecorder);

        if (recording) {
            trackRecorder.start();
        }
    }

    public void start() {
        trackRecorders.forEach(TrackRecorder::start);
        recording = true;
    }

    public void stop() {
        trackRecorders.forEach(TrackRecorder::stop);
        recording = false;
    }

    private static record TrackRecorder(String filename, MediaStreamTrack mediaStreamTrack) {
        void start() {
            // Start recording logic for the track
        }

        void stop() {
            // Stop recording logic for the track
        }
    }
}
