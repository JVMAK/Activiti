/* Licensed under the Apache License, Version 2.0 (the "License");
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
 */
package com.activiti.rest.editor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.model.BpmnModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.activiti.domain.editor.AbstractModel;
import com.activiti.domain.editor.Model;
import com.activiti.domain.editor.ModelHistory;
import com.activiti.security.SecurityUtils;
import com.activiti.service.editor.ModelInternalService;
import com.activiti.service.exception.BadRequestException;
import com.activiti.service.exception.BaseModelerRestException;
import com.activiti.service.exception.InternalServerErrorException;

/**
 * @author jbarrez
 */
public class AbstractModelBpmnResource extends BaseModelResource {

    private final Logger log = LoggerFactory.getLogger(AbstractModelBpmnResource.class);

	@Inject
	protected ModelInternalService modelService;

    public void getProcessModelBpmn20Xml(HttpServletResponse response, Long processModelId) throws IOException {

		if (processModelId == null) {
			throw new BadRequestException("No process model id provided");
		}

        Model model = getModel(processModelId, true, false);
        generateBpmn20Xml(response, model);
    }

    public void getHistoricProcessModelBpmn20Xml(HttpServletResponse response, Long processModelId, Long processModelHistoryId) throws IOException {

		if (processModelId == null) {
			throw new BadRequestException("No process model id provided");
		}

        ModelHistory historicModel = getModelHistory(processModelId, processModelHistoryId, true, false);
        generateBpmn20Xml(response, historicModel);
    }

	protected void generateBpmn20Xml(HttpServletResponse response, AbstractModel model) {
	    String name = model.getName().replaceAll(" ", "_");
		response.setHeader("Content-Disposition", "attachment; filename=" + name + ".bpmn20.xml");
        if (model.getModelEditorJson() != null) {
    	    try {
    	    	ServletOutputStream servletOutputStream = response.getOutputStream();
    	    	response.setContentType("application/xml");

                BpmnModel bpmnModel = modelService.getBpmnModel(model, SecurityUtils.getCurrentUserObject(), true);
                byte[] xmlBytes = modelService.getBpmnXML(bpmnModel);
                BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(xmlBytes));

        		byte[] buffer = new byte[8096];
        		while (true) {
        			int count = in.read(buffer);
        			if (count == -1)
        				break;
        			servletOutputStream.write(buffer, 0, count);
        		}

        		// Flush and close stream
        		servletOutputStream.flush();
        		servletOutputStream.close();

    	    } catch (BaseModelerRestException e) {
    	        throw e;

            } catch (Exception e) {
            	log.error("Could not generate BPMN 2.0 XML", e);
            	throw new InternalServerErrorException("Could not generate BPMN 2.0 xml");
            }
    	}
    }
}
