/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.webhook.service.code.filter

import org.slf4j.LoggerFactory

class UserFilter(
    private val pipelineId: String,
    private val triggerOnUser: String,
    private val includedUsers: List<String>,
    private val excludedUsers: List<String>
) : WebhookFilter {

    companion object {
        private val logger = LoggerFactory.getLogger(UserFilter::class.java)
    }

    override fun doFilter(response: WebhookFilterResponse): Boolean {
        logger.info(
            "$pipelineId|triggerOnUser:$triggerOnUser|includedUsers:$includedUsers" +
                "|excludedUsers:$excludedUsers|user filter"
        )
        return hasNoUserSpecs() || (isUserNotExcluded() && isUserIncluded())
    }

    private fun hasNoUserSpecs(): Boolean {
        return includedUsers.isEmpty() && excludedUsers.isEmpty()
    }

    private fun isUserNotExcluded(): Boolean {
        excludedUsers.forEach { excludeUser ->
            if (triggerOnUser.matches(Regex(excludeUser))) {
                logger.warn("$pipelineId|$excludeUser|the exclude user match the git event user")
                return false
            }
        }
        return true
    }

    private fun isUserIncluded(): Boolean {
        includedUsers.forEach { includedUser ->
            if (triggerOnUser.matches(Regex(includedUser))) {
                logger.warn("$pipelineId|includedUser|the included user match the git event user")
                return true
            }
        }
        return includedUsers.isEmpty()
    }
}
