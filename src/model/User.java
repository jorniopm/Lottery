package model;

public class User {
        private String id;
        private String name;
        private String photoPath;

        public User(String id, String name, String photoPath) {
            this.id = id;
            this.name = name;
            this.photoPath = photoPath;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhotoPath() { return photoPath; }

}
