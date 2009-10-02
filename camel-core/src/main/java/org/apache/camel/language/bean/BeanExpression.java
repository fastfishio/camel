/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.language.bean;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.component.bean.BeanHolder;
import org.apache.camel.component.bean.BeanProcessor;
import org.apache.camel.component.bean.ConstantBeanHolder;
import org.apache.camel.component.bean.RegistryBean;
import org.apache.camel.util.ObjectHelper;

/**
 * Evaluates an expression using a bean method invocation
 *
 * @version $Revision$
 */
public class BeanExpression implements Expression, Predicate {
    private String beanName;
    private String method;
    private Object bean;

    public BeanExpression(Object bean, String method) {
        this.bean = bean;
        this.method = method;
    }

    public BeanExpression(String beanName, String method) {
        this.beanName = beanName;
        this.method = method;
    }

    @Override
    public String toString() {
        return "BeanExpression[bean:" + (bean == null ? beanName : bean) + " method: " + method + "]";
    }

    public Object evaluate(Exchange exchange) {
        // either use registry lookup or a constant bean
        BeanHolder holder;
        if (bean == null) {
            holder = new RegistryBean(exchange.getContext(), beanName);
        } else {
            holder = new ConstantBeanHolder(bean, exchange.getContext());
        }

        BeanProcessor processor = new BeanProcessor(holder);
        if (method != null) {
            processor.setMethod(method);
        }
        try {
            Exchange newExchange = exchange.copy();
            // The BeanExperession always has a result regardless of the ExchangePattern,
            // so I add a checker here to make sure we can get the result.
            if (!newExchange.getPattern().isOutCapable()) {
                newExchange.setPattern(ExchangePattern.InOut);
            }
            processor.process(newExchange);
            return newExchange.getOut().getBody();
        } catch (Exception e) {
            throw new RuntimeBeanExpressionException(exchange, beanName, method, e);
        }
    }

    public <T> T evaluate(Exchange exchange, Class<T> type) {
        Object result = evaluate(exchange);
        return exchange.getContext().getTypeConverter().convertTo(type, result);
    }

    public boolean matches(Exchange exchange) {
        Object value = evaluate(exchange);
        return ObjectHelper.evaluateValuePredicate(value);
    }

}
