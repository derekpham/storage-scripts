package com.depham.runnnables;

import com.depham.factories.PhotosLibraryClientFactory;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.ListMediaItemsRequest;
import com.google.photos.library.v1.proto.SearchMediaItemsRequest;
import com.google.photos.types.proto.MediaMetadata;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Retrieves all photos that are not in any album
 */
public class RetrieveAlbumlessPhotos {

  private static final Set<String> MEDIA_IDS = new HashSet<>();
  private static final Map<String, Set<MediaMetadata>> FILE_NAME_TO_MEDIA_METADATA = new HashMap<>();
  private static final Map<String, Set<Long>> FILE_NAME_TO_CREATION_TIMES = new HashMap<>();

  public static void main(String[] args) {
    PhotosLibraryClient client = PhotosLibraryClientFactory.createClient(
        Collections.singletonList("https://www.googleapis.com/auth/photoslibrary.readonly"));

    initializeAllMediaItemsFromAllAlbums(client);
    System.out.printf("Initialized map. Number of media items: %s%n", FILE_NAME_TO_MEDIA_METADATA.values().stream()
        .mapToInt(Set::size)
        .sum());
    System.out.printf("Initialized set. Number of media ids: %s%n", MEDIA_IDS.size());

    findPhotosNotInAnyAlbum(client);

    client.close();
  }

  private static void initializeAllMediaItemsFromAllAlbums(PhotosLibraryClient client) {
    client.listAlbums().iterateAll().forEach(album -> {
      InternalPhotosLibraryClient.SearchMediaItemsPagedResponse response = client.searchMediaItems(
          SearchMediaItemsRequest.newBuilder().setPageSize(100).setAlbumId(album.getId()).build()
      );
      response.iterateAll().forEach(mediaItem -> {
        MEDIA_IDS.add(mediaItem.getId());

        String fileName = mediaItem.getFilename();
        FILE_NAME_TO_MEDIA_METADATA.putIfAbsent(fileName, new HashSet<>());
        FILE_NAME_TO_CREATION_TIMES.putIfAbsent(fileName, new HashSet<>());
        if (mediaItem.hasMediaMetadata()) {
          FILE_NAME_TO_MEDIA_METADATA.get(fileName).add(mediaItem.getMediaMetadata());

          if (mediaItem.getMediaMetadata().hasCreationTime()) {
            FILE_NAME_TO_CREATION_TIMES.get(fileName)
                .add(mediaItem.getMediaMetadata().getCreationTime().getSeconds());
          }
        }
      });
    });
  }

  /*
  Best effort because of edge cases where you save a photo that someone else uploaded, which will have
  a different media id from the original media id (with a different product url and base url)

  If so, we can still check the file name and the metadata. However, apparently even metadata might get
  slightly modified (some info might be removed). So we can check on the creation time in the metadata.
  If they have the same creation times, extremely likely the same photos.
   */
  private static void findPhotosNotInAnyAlbum(PhotosLibraryClient client) {
    client.listMediaItems(ListMediaItemsRequest.newBuilder().setPageSize(100).build())
        .iterateAll()
        .forEach(mediaItem -> {
          String fileName = mediaItem.getFilename();

          if (MEDIA_IDS.contains(mediaItem.getId())
              ||
              (FILE_NAME_TO_MEDIA_METADATA.containsKey(fileName)
                  && FILE_NAME_TO_MEDIA_METADATA.get(fileName).contains(mediaItem.getMediaMetadata()))
              ||
              (FILE_NAME_TO_CREATION_TIMES.containsKey(fileName)
                  && mediaItem.hasMediaMetadata()
                  && mediaItem.getMediaMetadata().hasCreationTime()
                  && FILE_NAME_TO_CREATION_TIMES.get(fileName).contains(mediaItem.getMediaMetadata().getCreationTime().getSeconds()))) {
            return;
          }

          System.out.println(mediaItem.getProductUrl());
        });
  }
}