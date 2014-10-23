/*
 * Copyright Gert Wijns gert.wijns@gmail.com
 *
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
 */
package be.shad.tsqb.values;

import be.shad.tsqb.NamedParameter;
import be.shad.tsqb.query.TypeSafeQuery;
import be.shad.tsqb.query.copy.CopyContext;
import be.shad.tsqb.query.copy.Copyable;

/**
 * The value is an actual value, not a proxy or property path.
 * This value is added as param to the query.
 */
public class DirectTypeSafeValue<T> extends TypeSafeValueImpl<T> implements NamedValueEnabled, DirectTypeSafeValueWrapper<T> {
    private T value;

    @SuppressWarnings("unchecked")
    public DirectTypeSafeValue(TypeSafeQuery query, T value) {
        this(query, (Class<T>) value.getClass());
        setValue(value);
    }

    public DirectTypeSafeValue(TypeSafeQuery query, Class<T> valueClass) {
        super(query, valueClass);
    }

    /**
     * Copy constructor
     */
    protected DirectTypeSafeValue(CopyContext context, DirectTypeSafeValue<T> original) {
        super(context, original);
        value = context.getOrOriginal(original.value);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public T getWrappedValue() {
        return value;
    }

    @Override
    public HqlQueryValueImpl toHqlQueryValue(HqlQueryBuilderParams params) {
        if (value == null) {
            throw new IllegalStateException("Value is null when transforming to query");
        }
        if (params.isRequiresLiterals()) {
            return new HqlQueryValueImpl(query.getHelper().toLiteral(getValue()));
        } else {
            String name = params.createNamedParameter();
            return new HqlQueryValueImpl(":" + name, new NamedParameter(name, getValue()));
        }
    }

    @Override
    public void setNamedValue(Object value) {
        if (value != null && !getValueClass().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format("The value must be of type "
                    + "[%s] but was of type [%s].", getValueClass(), value.getClass()));
        }
        this.value = getValueClass().cast(value);
    }

    @Override
    public Copyable copy(CopyContext context) {
        return new DirectTypeSafeValue(context, this);
    }

}
