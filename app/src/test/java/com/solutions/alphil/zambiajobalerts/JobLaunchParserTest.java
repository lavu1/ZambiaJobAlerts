package com.solutions.alphil.zambiajobalerts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JobLaunchParserTest {

    @Test
    public void extractIdentifier_readsJobPath() {
        assertEquals(
                "multiple-job-opportunities-8",
                JobLaunchParser.extractIdentifier(
                        "https",
                        "zambiajobalerts.com",
                        "/job/multiple-job-opportunities-8/"
                )
        );
    }

    @Test
    public void extractIdentifier_readsJobsPath() {
        assertEquals(
                "24635",
                JobLaunchParser.extractIdentifier(
                        "https",
                        "www.zambiajobalerts.com",
                        "/jobs/24635/"
                )
        );
    }

    @Test
    public void extractIdentifier_readsCustomSchemeHost() {
        assertEquals(
                "general-work-3",
                JobLaunchParser.extractIdentifier(
                        "zambiajobalerts",
                        "job",
                        "/general-work-3/"
                )
        );
    }

    @Test
    public void extractIdentifier_rejectsUnsupportedCustomHost() {
        assertNull(
                JobLaunchParser.extractIdentifier(
                        "zambiajobalerts",
                        "profile",
                        "/general-work-3/"
                )
        );
    }

    @Test
    public void extractIdentifier_rejectsUnsupportedWebHost() {
        assertNull(
                JobLaunchParser.extractIdentifier(
                        "https",
                        "example.com",
                        "/job/general-work-3/"
                )
        );
    }

    @Test
    public void isHomeUri_acceptsRootDomain() {
        assertEquals(
                true,
                JobLaunchParser.isHomeUri(
                        "https",
                        "zambiajobalerts.com",
                        "/"
                )
        );
    }

    @Test
    public void isHomeUri_acceptsRootDomainWithoutPath() {
        assertEquals(
                true,
                JobLaunchParser.isHomeUri(
                        "https",
                        "www.zambiajobalerts.com",
                        ""
                )
        );
    }

    @Test
    public void isHomeUri_rejectsJobPath() {
        assertEquals(
                false,
                JobLaunchParser.isHomeUri(
                        "https",
                        "zambiajobalerts.com",
                        "/jobs/24635/"
                )
        );
    }

    @Test
    public void buildJobDetailsBySlugUrl_encodesQueryValues() {
        assertEquals(
                "https://example.com/wp-json/wp/v2/job-listings?slug=C%2B%2B+Developer&_embed=1",
                JobLaunchParser.buildJobDetailsBySlugUrl(
                        "https://example.com/wp-json/wp/v2/job-listings",
                        "C++ Developer"
                )
        );
    }
}
