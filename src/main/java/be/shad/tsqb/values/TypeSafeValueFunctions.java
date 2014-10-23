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

import java.util.Date;

import be.shad.tsqb.query.TypeSafeQueryInternal;

/**
 * Provides a bunch of functions, this list may grow in time.
 */
public class TypeSafeValueFunctions {
    private final TypeSafeQueryInternal query;

    public TypeSafeValueFunctions(TypeSafeQueryInternal query) {
        this.query = query;
    }
    
    public <VAL> CaseTypeSafeValue<VAL> caseWhen(Class<VAL> valueClass) {
        return new CaseTypeSafeValue<VAL>(query, valueClass);
    }
    
    public <VAL> TypeSafeValue<VAL> distinct(VAL val) {
        return distinct(query.toValue(val));
    }

    public <VAL> TypeSafeValue<VAL> distinct(TypeSafeValue<VAL> val) {
        return new DistinctTypeSafeValue<VAL>(query, val);
    }

    public TypeSafeValue<Long> count() {
        return new CustomTypeSafeValue<Long>(query, Long.class, "count(*)");
    }
    
    public <VAL> TypeSafeValue<Long> countDistinct(VAL val) {
        return countDistinct(query.toValue(val));
    }

    public <VAL> TypeSafeValue<Long> countDistinct(TypeSafeValue<VAL> val) {
        return new CountTypeSafeValue(query, distinct(val));
    }
    
    public <VAL, CAST> TypeSafeValue<CAST> cast(VAL val, Class<CAST> type) {
        return cast(query.toValue(val), type);
    }

    public <VAL, CAST> TypeSafeValue<CAST> cast(TypeSafeValue<VAL> val, Class<CAST> type) {
        return new CastTypeSafeValue<CAST>(query, type, val);
    }
    
    public <VAL> CoalesceTypeSafeValue<VAL> coalesce(VAL val) {
        return coalesce(query.toValue(val));
    }
    
    public <VAL> CoalesceTypeSafeValue<VAL> coalesce(TypeSafeValue<VAL> val) {
        CoalesceTypeSafeValue<VAL> coalesce = new CoalesceTypeSafeValue<VAL>(query, val.getValueClass());
        coalesce.or(val);
        return coalesce;
    }

    public TypeSafeValue<String> upper(String val) {
        return upper(query.toValue(val));
    }

    public TypeSafeValue<String> upper(TypeSafeValue<String> val) {
        return new WrappedTypeSafeValue<String>(query, "upper", val);
    }
    
    public TypeSafeValue<String> lower(String val) {
        return lower(query.toValue(val));
    }

    public TypeSafeValue<String> lower(TypeSafeValue<String> val) {
        return new WrappedTypeSafeValue<String>(query, "lower", val);
    }

    public <N extends Number> TypeSafeValue<N> min(N n) {
        return minn(query.toValue(n));
    }

    public <N extends Number> TypeSafeValue<N> minn(TypeSafeValue<N> nv) {
        return new WrappedTypeSafeValue<N>(query, "min", nv);
    }
    
    public TypeSafeValue<Date> max(Date n) {
        return maxd(query.toValue(n));
    }

    public TypeSafeValue<Date> maxd(TypeSafeValue<Date> nv) {
        return new WrappedTypeSafeValue<Date>(query, "max", nv);
    }
    
    public TypeSafeValue<Date> min(Date n) {
        return mind(query.toValue(n));
    }

    public TypeSafeValue<Date> mind(TypeSafeValue<Date> nv) {
        return new WrappedTypeSafeValue<Date>(query, "min", nv);
    }
    
    public <N extends Number> TypeSafeValue<N> max(N n) {
        return maxn(query.toValue(n));
    }

    public <N extends Number> TypeSafeValue<N> maxn(TypeSafeValue<N> nv) {
        return new WrappedTypeSafeValue<N>(query, "max", nv);
    }
    
    public <N extends Number> TypeSafeValue<N> avg(N n) {
        return avg(query.toValue(n));
    }

    public <N extends Number> TypeSafeValue<N> avg(TypeSafeValue<N> nv) {
        return new WrappedTypeSafeValue<N>(query, "avg", nv);
    }
    
    public <N extends Number> TypeSafeValue<N> sum(N n) {
        return sum(query.toValue(n));
    }

    public <N extends Number> TypeSafeValue<N> sum(TypeSafeValue<N> nv) {
        return new WrappedTypeSafeValue<N>(query, "sum", nv);
    }

    /**
     * Wrapps the value in brackets.
     */
    public <N> TypeSafeValue<N> wrap(TypeSafeValue<N> value) {
        return new WrappedTypeSafeValue<N>(query, "", value);
    }
    
}
