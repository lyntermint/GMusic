package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.object.GSong;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

public class SongService {

    private final GMusicMain gMusicMain;
    private static final List<Material> DISCS = Arrays.stream(Material.values()).filter(disc -> disc.name().contains("_DISC_")).toList();
    private final TreeMap<String, GSong> songs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public SongService(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    public List<GSong> getSongs() { return new ArrayList<>(songs.values()); }

    public GSong getSongById(String song) { return songs.get(song.toLowerCase()); }

    public List<GSong> filterSongsBySearch(List<GSong> songs, String search) { return songs.stream().filter(song -> song.getTitle().toLowerCase().contains(search.toLowerCase())).toList(); }

    public void loadSongs() {
        unloadSongs();

        convertAllSongs();

        File songsDir = new File(gMusicMain.getDataFolder(), "songs");
        if(!songsDir.exists()) return;

        File[] songFiles = songsDir.listFiles();
        if(songFiles == null) return;
        for(File file : songFiles) {
            int extensionPos = file.getName().lastIndexOf(".");
            if(extensionPos <= 0 || !file.getName().substring(extensionPos + 1).equalsIgnoreCase("gnbs")) return;

            try {
                GSong song = new GSong(file);
                if(song.getNoteAmount() == 0) {
                    gMusicMain.getLogger().warning("Could not load song '" + file.getName().substring(0, extensionPos) + "', no notes found");
                    continue;
                }

                songs.put(song.getId().toLowerCase(), song);
            } catch(Throwable e) {
                gMusicMain.getLogger().log(Level.WARNING, "Could not load song '" + file.getName().substring(0, extensionPos) + "'", e);
            }
        }
    }

    public void unloadSongs() {
        songs.clear();
    }

    private void convertAllSongs() {
        File songsDir = new File(gMusicMain.getDataFolder(), "songs");
        if(!songsDir.exists() && !songsDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create 'songs' directory!");
            return;
        }

        File nbsDir = new File(gMusicMain.getDataFolder(), "nbs");
        if(!nbsDir.exists() && !nbsDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create 'nbs' directory!");
            return;
        }

        File midiDir = new File(gMusicMain.getDataFolder(), "midi");
        if(!midiDir.exists() && !midiDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create 'midi' directory!");
            return;
        }

        syncMidiCache(songsDir, midiDir);

        File[] nbsFiles = nbsDir.listFiles();
        if(nbsFiles != null) {
            for(File file : nbsFiles) {
                File songFile = new File(songsDir.getAbsolutePath() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + ".gnbs");
                if(songFile.exists()) continue;
                gMusicMain.getNBSConverter().convertNBSFile(file);
            }
        }

        File[] midiFiles = midiDir.listFiles();
        if(midiFiles == null) return;
        for(File file : midiFiles) {
            File songFile = new File(songsDir.getAbsolutePath() + "/" + file.getName().replaceFirst("[.][^.]+$", "") + ".gnbs");
            if(songFile.exists()) {
                YamlConfiguration gnbs = YamlConfiguration.loadConfiguration(songFile);
                String source = gnbs.getString("Song.Source", "");
                String sourceFile = gnbs.getString("Song.SourceFile", "");
                String expected = file.getName().replaceFirst("[.][^.]+$", "");
                boolean needsSourceTag = !"MIDI".equalsIgnoreCase(source) || !expected.equalsIgnoreCase(sourceFile);
                if(!needsSourceTag && songFile.lastModified() >= file.lastModified()) continue;
            }
            gMusicMain.getMidiConverter().convertMidiFile(file);
        }
    }

    private void syncMidiCache(File songsDir, File midiDir) {
        File cacheFile = new File(gMusicMain.getDataFolder(), "midi_cache.yml");
        YamlConfiguration cache = YamlConfiguration.loadConfiguration(cacheFile);
        List<String> previous = cache.getStringList("midi");

        Set<String> current = new HashSet<>();
        File[] midiFiles = midiDir.listFiles();
        if(midiFiles != null) {
            for(File file : midiFiles) {
                if(file.isDirectory()) continue;
                String name = file.getName();
                int extensionPos = name.lastIndexOf(".");
                if(extensionPos > 0) name = name.substring(0, extensionPos);
                current.add(name);
            }
        }

        for(String name : previous) {
            if(current.contains(name)) continue;
            File songFile = new File(songsDir, name + ".gnbs");
            if(songFile.exists() && !songFile.delete()) {
                gMusicMain.getLogger().warning("Could not remove song '" + name + "' from songs directory");
            }
        }

        File[] songFiles = songsDir.listFiles();
        if(songFiles != null) {
            for(File songFile : songFiles) {
                int extensionPos = songFile.getName().lastIndexOf(".");
                if(extensionPos <= 0 || !songFile.getName().substring(extensionPos + 1).equalsIgnoreCase("gnbs")) continue;
                YamlConfiguration gnbs = YamlConfiguration.loadConfiguration(songFile);
                if(!"MIDI".equalsIgnoreCase(gnbs.getString("Song.Source", ""))) continue;
                String sourceFile = gnbs.getString("Song.SourceFile", "");
                if(sourceFile.isEmpty()) continue;
                if(!current.contains(sourceFile)) {
                    if(!songFile.delete()) {
                        gMusicMain.getLogger().warning("Could not remove song '" + sourceFile + "' from songs directory");
                    }
                }
            }
        }

        cache.set("midi", new ArrayList<>(current));
        try {
            cache.save(cacheFile);
        } catch(Throwable e) {
            gMusicMain.getLogger().log(Level.WARNING, "Could not save midi cache", e);
        }
    }

}
