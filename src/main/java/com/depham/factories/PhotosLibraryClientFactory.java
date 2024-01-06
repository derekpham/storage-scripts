package com.depham.factories;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;


public class PhotosLibraryClientFactory {

  private static final java.io.File DATA_STORE_DIR = new java.io.File(
      PhotosLibraryClientFactory.class.getResource("/").getPath(), "credentials");
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final int LOCAL_RECEIVER_PORT = 61984;

  private PhotosLibraryClientFactory() { }

  public static PhotosLibraryClient createClient(List<String> selectedScopes) {
    String credentialsPath = PhotosLibraryClientFactory.class.getClassLoader().getResource("googleApiCredentials.json").getPath();
    System.out.println(credentialsPath);
    try {
      PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
          .setCredentialsProvider(FixedCredentialsProvider.create(getUserCredentials(credentialsPath, selectedScopes)))
          .build();
      return PhotosLibraryClient.initialize(settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Credentials getUserCredentials(String credentialsPath, List<String> selectedScopes) {
    try {
      GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
          JSON_FACTORY, new InputStreamReader(Files.newInputStream(Paths.get(credentialsPath))));

      String clientId = clientSecrets.getDetails().getClientId();
      String clientSecret = clientSecrets.getDetails().getClientSecret();

      GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
          GoogleNetHttpTransport.newTrustedTransport(),
          JSON_FACTORY,
          clientSecrets,
          selectedScopes
      ).setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR))
          .setAccessType("offline")
          .build();

      LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(LOCAL_RECEIVER_PORT).build();
      Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

      return UserCredentials.newBuilder()
          .setClientId(clientId)
          .setClientSecret(clientSecret)
          .setRefreshToken(credential.getRefreshToken())
          .build();
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
