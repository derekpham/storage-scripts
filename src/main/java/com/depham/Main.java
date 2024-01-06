package com.depham;

import com.depham.factories.PhotosLibraryClientFactory;
import com.google.photos.library.v1.PhotosLibraryClient;
import java.util.Collections;


public class Main {
  public static void main(String[] args) {
    try (PhotosLibraryClient client = PhotosLibraryClientFactory.createClient(
        Collections.singletonList("https://www.googleapis.com/auth/photoslibrary.readonly"))) {
      client.listAlbums().iteratePages().forEach(
          listAlbumsPage -> listAlbumsPage.getResponse().getAlbumsList().forEach(album -> System.out.println(album.getTitle()))
      );
    }
  }
}