package com.requirementyogi.datacenter.psea.web;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.xwork.XsrfTokenGenerator;
import com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO;
import com.requirementyogi.datacenter.psea.dto.DTOPseaTask;
import com.requirementyogi.datacenter.psea.impl.PseaServiceImpl;
import com.requirementyogi.datacenter.utils.confluence.compat.CompatibilityLayer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.requirementyogi.datacenter.psea.db.dao.PseaTaskDAO.ITEMS_IN_UI;

public class PseaAdminAction extends ConfluenceActionSupport {

    private static final Logger log = LoggerFactory.getLogger(PseaAdminAction.class);

    private final long SIZE_K = 1000L; // Don't set 1024, it's not bytes
    private final long SIZE_M = 1000000L;
    private final long SIZE_G = 1000000000L;
    private final Pattern NUMBERS_WITH_UNIT = Pattern.compile("([0-9]+)(\\s*[A-Za-z]*)");

    private PseaServiceImpl pseaService;
    private PseaTaskDAO pseaTaskDAO;
    private PermissionManager permissionManager;
    private XsrfTokenGenerator xsrfTokenGenerator;
    private CompatibilityLayer compatibilityLayer;

    /** The limit for the pagination */
    private Integer limit;

    private String timeLimit;
    private String dataLimit;
    private String rowLimit;
    private Long concurrentJobs;

    @Override
    public boolean isPermitted() {
        return super.isPermitted()
                && permissionManager.isConfluenceAdministrator(AuthenticatedUserThreadLocal.get());
    }

    public String doAdmin() {
        rowLimit = convertSizeToHuman(pseaService.getRowLimit());
        timeLimit = convertTimeToHuman(pseaService.getTimeLimit());
        dataLimit = convertSizeToHuman(pseaService.getDataLimit());
        concurrentJobs = pseaService.getConcurrentJobsLimit();
        return INPUT;
    }

    public String doSave() {
        if (!validateToken()) {
            return INPUT;
        }
        if (!isPermitted()) throw new IllegalStateException("Permissions haven't been checked properly.");
        pseaService.setDataLimit(convertSizeToMachine(dataLimit));
        pseaService.setRowLimit(convertSizeToMachine(rowLimit));
        pseaService.setTimeLimit(convertTimeToMachine(timeLimit));
        pseaService.setConcurrentJobsLimit(concurrentJobs);
        addActionMessage("Saved");
        return doAdmin();
    }

    /** Validates the XSRF Token, because this dumbass Confluence accepts anything even though we are in a 'validatingStack'.
     * @return*/
    private boolean validateToken() {
        HttpServletRequest request = compatibilityLayer.getRequest();
        if (request == null) {
            addActionError("The HTTP request is missing. Please report your problem to the authors of Requirement Yogi.");
            return false;
        }
        String token = request.getParameter("atl_token");
        if (!xsrfTokenGenerator.validateToken(request, token)) {
            addActionError("The XSRF token is missing or invalid. Most often, you can reload the previous" +
                    " page and perform the action again. If you were redirected here from a website or an email," +
                    " then it is possible that this website/email wanted to trick Confluence into performing a" +
                    " change in your name.");
            return false;
        }
        return true;
    }

    Long convertTimeToMachine(String time) {
        if (StringUtils.isBlank(time)) return null;
        Matcher matcher = NUMBERS_WITH_UNIT.matcher(time);
        if (!matcher.find()) return null;
        try {
            long value = Long.parseLong(matcher.group(1));
            return TimeUnit.SECONDS.toMillis(value); // We tell the user the enter the value in seconds.
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    String convertTimeToHuman(Long time) {
         if (time == null) return null;
         return TimeUnit.MILLISECONDS.toSeconds(time) + " s";
    }

    Long convertSizeToMachine(String size) {
        size = size.trim();
        Matcher matcher = NUMBERS_WITH_UNIT.matcher(size);
        if (!matcher.find()) return null;
        long value;
        try {

            value = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException nfe) {
            return null;
        }
        String unit = matcher.group(2);
        if (unit != null) {
            unit = unit.trim();
            switch (unit) {
                case "K":
                case "KB":
                    value = value * SIZE_K;
                    break;
                case "M":
                case "MB":
                    value = value * SIZE_M;
                    break;
                case "G":
                case "GB":
                    value = value * SIZE_G;
                    break;
            }
        }
        return value;
    }

    String convertSizeToHuman(Long size) {
        if (size == null) return null;
        if (size >= SIZE_G) {
            return size / SIZE_G + " G";
        } else if (size >= SIZE_M) {
            return size / SIZE_M + " M";
        } else if (size >= SIZE_K) {
            return size / SIZE_K + " K";
        } else {
            return String.valueOf(size);
        }
    }

    public List<DTOPseaTask> getLastExportList() {
        return pseaTaskDAO.getList(limit == null ? ITEMS_IN_UI : limit);
    }

    public void setPseaTaskDAO(PseaTaskDAO pseaTaskDAO) {
        this.pseaTaskDAO = pseaTaskDAO;
    }

    public String getRowLimitDefault() {
        return convertSizeToHuman(PseaServiceImpl.MAX_ROWS_DEFAULT);
    }

    public String getTimeLimitDefault() {
        return convertTimeToHuman(PseaServiceImpl.TIME_LIMIT_DEFAULT);
    }

    public String getTimeLimitMax() {
        return convertTimeToHuman(PseaServiceImpl.TIME_LIMIT_MAX);
    }

    public String getRowLimit() {
        return rowLimit;
    }

    public String getTimeLimit() {
        return timeLimit;
    }

    public String getDataLimit() {
        return dataLimit;
    }

    public String getDataLimitDefault() {
        return convertSizeToHuman(PseaServiceImpl.DATA_LIMIT_DEFAULT);
    }

    public Long getConcurrentJobs() {
        return concurrentJobs;
    }

    public Long getConcurrentJobsDefault() {
        return PseaServiceImpl.CONCURRENT_JOBS_DEFAULT;
    }

    public void setConcurrentJobs(Long concurrentJobs) {
        this.concurrentJobs = concurrentJobs;
    }

    public void setTimeLimit(String timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setDataLimit(String dataLimit) {
        this.dataLimit = dataLimit;
    }

    public void setRowLimit(String rowLimit) {
        this.rowLimit = rowLimit;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public void setCompatibilityLayer(CompatibilityLayer compatibilityLayer) {
        this.compatibilityLayer = compatibilityLayer;
    }

    @Override
    public void setPermissionManager(@ComponentImport PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public void setPseaService(PseaServiceImpl pseaService) {
        this.pseaService = pseaService;
    }

    public void setXsrfTokenGenerator(@ComponentImport XsrfTokenGenerator xsrfTokenGenerator) {
        this.xsrfTokenGenerator = xsrfTokenGenerator;
    }
}
