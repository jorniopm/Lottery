package model;

import javafx.scene.image.Image;

public class User {
        private String id;
        private String name;
        private String photoPath;
        private Image roundedImage; // 缓存圆角头像

        public User(String id, String name, String photoPath) {
            this.id = id;
            this.name = name;
            this.photoPath = photoPath;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhotoPath() { return photoPath; }
    public Image getRoundedImage() { return roundedImage; }
    public void setRoundedImage(Image roundedImage) { this.roundedImage = roundedImage; }
}
