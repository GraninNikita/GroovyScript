
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.worklog.WorklogImpl
import com.atlassian.jira.config.properties.APKeys

finalMessage = ""

def mainMethod() {
    // This script can be run from Jira -> Administration -> Add-ons -> Script Console
    def jqlQuery = "issue in watchedIssues()"
    logMessage "Executing JQL: <pre>${jqlQuery}</pre>"
    def issues = findIssues(jqlQuery)
    deleteOtherWatchersFromIssues(issues)

    logMessage "Found ${issues.size()} issues. Here they are:"
    logIssues(issues)

    return finalMessage // the returned value is shown after the execution in Jira's Web Script Console
}


def findIssues(String jqlQuery) {
    def currentUser = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
    def issueManager = ComponentAccessor.issueManager
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider.class)
    def query = jqlQueryParser.parseQuery(jqlQuery)
    def results = searchProvider.search(query, currentUser, PagerFilter.unlimitedFilter)
    results.issues.collect { issue -> issueManager.getIssueObject(issue.id) }
}

def deleteOtherWatchersFromIssues(Collection<MutableIssue> issues) {
    def currentUser = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
    def issueManager = ComponentAccessor.issueManager
    def watcherManager = ComponentAccessor.watcherManager
    def worklogManager = ComponentAccessor.worklogManager

    issues.each { issue ->
        issueManager.getWatchersFor(issue).each {
            watcher ->
                if (watcher != currentUser) {
                    watcherManager.stopWatching(watcher, issue)
                    worklog = createWorkLog(issue, 1)
                    worklogManager.create(currentUser, worklog, 1, false)
                }

        }
    }
}

def createWorkLog(MutableIssue issue, int hours) {
    def worklogManager = ComponentAccessor.worklogManager

    worklog = new WorklogImpl(
            worklogManager,
            issue,
            null,
            issue.reporter.name,
            issue.summary,
            new Date(),
            null,
            null,
            hours * 3600
    )
}

def logIssues(Collection<MutableIssue> issues) {
    logMessage "<pre>"
    issues.each { issue -> logMessage formatIssue(issue) }
    logMessage "</pre>"
}

def logMessage(Object message) {
    finalMessage += "${message}<br/>"
}


def formatIssue(MutableIssue issue) {
    def issueLink = getIssueLink(issue)
    def htmlLink = "<a href=\"${issueLink}\">${issue.key}</a>"
    "<strong>${htmlLink}</strong> - ${issue.summary}"
}

def getIssueLink(MutableIssue issue) {
    def properties = ComponentAccessor.applicationProperties
    def jiraBaseUrl = properties.getString(APKeys.JIRA_BASEURL)
    "${jiraBaseUrl}/browse/${issue.key}"
}


mainMethod()