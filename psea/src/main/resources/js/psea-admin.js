"use strict";

AJS.$(function($) {
    $(".delete-job").on("click", function(e) {
        e.preventDefault();
        var $this = $(this);
        var $tr = $this.closest("tr");
        var jobId = $tr.attr("data-job-id");
        if (!isNaN(parseInt(jobId))) {
            $tr.find(".actions").html('<aui-spinner size="small">Deleting</aui-spinner>');
            $.ajax({
                url: AJS.contextPath() + "/rest/psea/1/jobs/" + jobId,
                type: "DELETE",
                contentType: "application/json; charset=utf-8",
                success: function (data) {
                    $tr.find(".actions").text(data);
                },
                error: function (jqXHR) {
                    $tr.find(".actions").text("Error while deleting the job: " +
                        (RY.extractMessageFromXHR && RY.extractMessageFromXHR(jqXHR, 300) || "")
                    );
                }
            });
        }
    });
});
