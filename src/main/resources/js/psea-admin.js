/*-
 * #%L
 * PSEA
 * %%
 * Copyright (C) 2016 - 2022 Requirement Yogi S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
