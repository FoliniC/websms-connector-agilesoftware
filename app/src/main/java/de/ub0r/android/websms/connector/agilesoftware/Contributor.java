package de.ub0r.android.websms.connector.agilesoftware;

/**
 * Created by U570062 on 12/5/2016.
 */
public class Contributor {

    String login;
    String html_url;

    int contributions;

    @Override
    public String toString() {
        return login + " (" + contributions + ")";
    }
}
