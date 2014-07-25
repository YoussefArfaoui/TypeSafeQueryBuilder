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
package be.shad.tsqb.restrictions.predicate;

import be.shad.tsqb.query.copy.Stateless;
import be.shad.tsqb.values.DirectTypeSafeStringValue;
import be.shad.tsqb.values.TypeSafeValue;

/**
 * Ignores direct text values with a null or empty text.
 */
public final class IgnoreDirectEmptyStringPredicate implements RestrictionPredicate, Stateless {

    @Override
    public boolean isValueApplicable(TypeSafeValue<?> value) {
        if (value instanceof DirectTypeSafeStringValue) {
            return !((DirectTypeSafeStringValue) value).isEmpty();
        }
        return true;
    }

}
