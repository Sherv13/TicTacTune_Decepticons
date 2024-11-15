package com.leviste;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class FirebaseService {
    private final DatabaseReference database;

    public FirebaseService() {
        try {
            FileInputStream serviceAccount = new FileInputStream("tictactune-273d7-firebase-adminsdk-grda9-d37179f201.json");
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://tictactune-273d7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .build();
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            database = FirebaseDatabase.getInstance().getReference();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    public void createUser(String username, String password, BooleanCallback callback) {
        // First check if username already exists
        database.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    callback.onCallback(false); // Username already exists
                } else {
                    // Create new user
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("password", password); // In a real app, you should hash the password
                    userData.put("createdAt", ServerValue.TIMESTAMP);
                    
                    database.child("users").child(username).setValue(userData,
                        (error, ref) -> callback.onCallback(error == null));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Error checking username: " + error.getMessage());
                callback.onCallback(false);
            }
        });
    }

    public void verifyLogin(String username, String password, BooleanCallback callback) {
        database.child("users").child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    callback.onCallback(storedPassword != null && storedPassword.equals(password));
                } else {
                    callback.onCallback(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Error verifying login: " + error.getMessage());
                callback.onCallback(false);
            }
        });
    }

    public void createRoom(String roomId, Runnable onSuccess) {
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("status", "waiting");
        
        database.child("rooms").child(roomId).setValue(roomData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error == null) {
                    onSuccess.run();
                } else {
                    System.err.println("Error creating room: " + error.getMessage());
                }
            }
        });
    }

    public void joinRoom(String roomId, BooleanCallback callback) {
        database.child("rooms").child(roomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onCallback(snapshot.exists());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Error checking room existence: " + error.getMessage());
                callback.onCallback(false);
            }
        });
    }

    public void makeMove(String roomId, int row, int col, String value) {
        Map<String, Object> move = new HashMap<>();
        move.put("row", row);
        move.put("col", col);
        move.put("value", value);
        
        database.child("rooms").child(roomId).child("moves").push().setValue(move, 
            new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        System.err.println("Error making move: " + error.getMessage());
                    }
                }
            });
    }

    public void skipCurrentSong(String roomId) {
        database.child("rooms").child(roomId).child("skipRequested").setValue(true,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error requesting skip: " + error.getMessage());
                }
            });
    }


    public void listenForMoves(String roomId, MoveCallback callback) {
        database.child("rooms").child(roomId).child("moves")
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    Map<String, Object> move = (Map<String, Object>) snapshot.getValue();
                    if (move != null) {
                        long row = (long) move.get("row");
                        long col = (long) move.get("col");
                        String value = (String) move.get("value");
                        callback.onMove((int) row, (int) col, value);
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {}

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Move listener cancelled: " + error.getMessage());
                }
            });
    }

    public void cleanupRoom(String roomId) {
        database.child("rooms").child(roomId).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    System.err.println("Error cleaning up room: " + error.getMessage());
                }
            }
        });
    }

    public void updateMusicState(String roomId, String state) {
        database.child("rooms").child(roomId).child("music").child("state").setValue(state,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error updating music state: " + error.getMessage());
                }
            });
    }

    public void updateVolume(String roomId, double volume) {
        database.child("rooms").child(roomId).child("music").child("volume").setValue(volume,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error updating volume: " + error.getMessage());
                }
            });
    }

    public void updateMusicUrl(String roomId, String url) {
        database.child("rooms").child(roomId).child("music").child("url").setValue(url,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error updating music URL: " + error.getMessage());
                }
            });
    }

    public void listenForMusicUpdates(String roomId, MusicUpdateCallback callback) {
        database.child("rooms").child(roomId)
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    handleMusicUpdate(snapshot, callback);
                }
    
                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    handleMusicUpdate(snapshot, callback);
                    
                    // Handle skip requests
                    if (snapshot.getKey().equals("skipRequested") && 
                        Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                        
                        // Clear the skip request
                        database.child("rooms").child(roomId).child("skipRequested").removeValue(null);
                        
                        // Get the next song from queue
                        database.child("rooms").child(roomId).child("queue")
                            .orderByKey()
                            .limitToFirst(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot queueSnapshot) {
                                    if (queueSnapshot.exists()) {
                                        for (DataSnapshot songSnapshot : queueSnapshot.getChildren()) {
                                            // Remove the song from queue
                                            songSnapshot.getRef().removeValue(null);
                                        }
                                    }
                                }
                                
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    System.err.println("Error processing skip: " + error.getMessage());
                                }
                            });
                    }
                }
    
                @Override
                public void onChildRemoved(DataSnapshot snapshot) {}
    
                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
    
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Music listener cancelled: " + error.getMessage());
                }
            });
    }

    private void handleMusicUpdate(DataSnapshot snapshot, MusicUpdateCallback callback) {
        switch (snapshot.getKey()) {
            case "skipRequested":
                if (Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    callback.onMusicStateUpdate("NEXT");
                    // Clear the skip request
                    database.child("rooms").child(snapshot.getRef().getParent().getKey())
                        .child("skipRequested").removeValue(null);
                }
                break;
            case "state":
                callback.onMusicStateUpdate(snapshot.getValue(String.class));
                break;
            case "volume":
                callback.onVolumeUpdate(snapshot.getValue(Double.class));
                break;
            case "url":
                callback.onMusicUrlUpdate(snapshot.getValue(String.class));
                break;
        }
    }

    public void listenForSongs(SongListCallback callback) {
        database.child("songs").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<String, Song> songs = new HashMap<>();
                for (DataSnapshot songSnapshot : snapshot.getChildren()) {
                    String key = songSnapshot.getKey();
                    String title = songSnapshot.child("title").getValue(String.class);
                    String url = songSnapshot.child("url").getValue(String.class);
                    songs.put(key, new Song(title, url));
                }
                callback.onSongsLoaded(songs);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Error loading songs: " + error.getMessage());
            }
        });
    }

    public void updateCurrentSong(String roomId, String songKey, String songTitle, String songUrl) {
        Map<String, Object> songData = new HashMap<>();
        songData.put("key", songKey);
        songData.put("title", songTitle);
        songData.put("url", songUrl);
        
        database.child("rooms").child(roomId).child("currentSong").setValue(songData,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error updating current song: " + error.getMessage());
                }
            });
    }


    public void getCurrentMusicState(String roomId, MusicStateCallback callback) {
        database.child("rooms").child(roomId).child("music").child("state")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String state = snapshot.getValue(String.class);
                    if (state != null) {
                        callback.onStateReceived(state);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Error getting music state: " + error.getMessage());
                }
            });
    }

    public interface MusicStateCallback {
        void onStateReceived(String state);
    }
    public interface SongListCallback {
        void onSongsLoaded(Map<String, Song> songs);
    }


    // Add Song class
    public static class Song {
        private final String title;
        private final String url;

        public Song(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getUrl() { return url; }
    }



    public interface MusicUpdateCallback {
        void onMusicStateUpdate(String state);
        void onVolumeUpdate(double volume);
        void onMusicUrlUpdate(String url);
    }

    public interface BooleanCallback {
        void onCallback(boolean value);
    }

    public interface MoveCallback {
        void onMove(int row, int col, String value);
    }
    public void addToQueue(String roomId, String songKey, String songTitle, String songUrl) {
        Map<String, Object> songData = new HashMap<>();
        songData.put("key", songKey);
        songData.put("title", songTitle);
        songData.put("url", songUrl);
        
        database.child("rooms").child(roomId).child("queue").push().setValue(songData,
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error adding song to queue: " + error.getMessage());
                }
            });
    }

    public void removeFromQueue(String roomId, String queueItemKey) {
        database.child("rooms").child(roomId).child("queue").child(queueItemKey).removeValue(
            (error, ref) -> {
                if (error != null) {
                    System.err.println("Error removing song from queue: " + error.getMessage());
                }
            });
    }

    public void listenToQueue(String roomId, QueueUpdateCallback callback) {
        database.child("rooms").child(roomId).child("queue")
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    String key = snapshot.getKey();
                    Map<String, Object> songData = (Map<String, Object>) snapshot.getValue();
                    
                    // Verify this is a new song before calling callback
                    database.child("rooms").child(roomId).child("queue").child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot verifySnapshot) {
                                if (verifySnapshot.exists()) {
                                    callback.onSongAdded(key, songData);
                                }
                            }
    
                            @Override
                            public void onCancelled(DatabaseError error) {
                                System.err.println("Queue verification cancelled: " + error.getMessage());
                            }
                        });
                }
    
                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    String key = snapshot.getKey();
                    callback.onSongRemoved(key);
                }
    
                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
                
                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
                
                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Queue listener cancelled: " + error.getMessage());
                }
            });
    }


    public interface QueueUpdateCallback {
        void onSongAdded(String key, Map<String, Object> songData);
        void onSongRemoved(String key);
    }
}