package com.arpan.services;

import com.arpan.model.NumberOfIssuesModel;
import com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.egit.github.core.RepositoryIssue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.NoSuchPageException;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.arpan.util.IssuesUtil.createGitApiUrl;
import static com.arpan.util.IssuesUtil.getNumberOfIssuesModel;

@Slf4j
@Service
public class IssuesService {
    private static final String ISSUES = "/issues";
    private final GitHubClient gitHubClient;
    @Value("${auth-token:1234}")
    private String githubToken;

    @Autowired
    public IssuesService() {
        this.gitHubClient = new GitHubClient();
    }

    private PageIterator<RepositoryIssue> pageIssues(String uri) {
        PagedRequest<RepositoryIssue> request = new PagedRequest<>(1, 100);
        request.setUri(uri);
        request.setType(new TypeToken<List<RepositoryIssue>>() {
        }.getType());
        return new PageIterator<>(request, gitHubClient);
    }

    private <V> List<V> getAll(PageIterator<V> iterator, List<String> errorList) {
        List<V> elements = new ArrayList<V>();
        try {
            while (iterator.hasNext())
                elements.addAll(iterator.next());
        } catch (NoSuchPageException pageException) {
            errorList.add("No page for this request exists.");
            return Collections.emptyList();
        }
        return elements;
    }

    /**
     * This method gives {@link Optional<NumberOfIssuesModel>} from a given git Url.
     *
     * @param gitRepoUrl Git Repository Url.
     * @return Optional of Issues Model.
     */
    public Optional<NumberOfIssuesModel> getIssuesModel(String gitRepoUrl) {
        gitHubClient.setOAuth2Token(githubToken);
        List<String> errorList = new ArrayList<>();

        String gitApiUrl = createGitApiUrl(gitRepoUrl, ISSUES);
        if (Strings.isNullOrEmpty(gitApiUrl)) {
            log.error("Received Null or Empty URL");
            return Optional.empty();
        }
        List<RepositoryIssue> repositoryIssues = getAll(pageIssues(gitApiUrl), errorList);
        if (!errorList.isEmpty()) {
            return Optional.empty();
        }
        return getNumberOfIssuesModel(repositoryIssues);
    }
}