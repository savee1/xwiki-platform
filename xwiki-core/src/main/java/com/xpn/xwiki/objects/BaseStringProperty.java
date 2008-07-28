/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki.objects;

public class BaseStringProperty extends BaseProperty implements Cloneable
{
    private String value;

    public BaseStringProperty()
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#getValue()
     */
    public Object getValue()
    {
        return this.value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#setValue(java.lang.Object)
     */
    public void setValue(Object value)
    {
        this.value = (String) value;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#toText()
     */
    public String toText()
    {
        String value = (String) getValue();
        if (value != null) {
            return value;
        }

        return "";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (!super.equals(obj)) {
            return false;
        }

        if ((getValue() == null) && (((BaseStringProperty) obj).getValue() == null)) {
            return true;
        }

        return getValue().equals(((BaseStringProperty) obj).getValue());
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseProperty#clone()
     */
    public Object clone()
    {
        BaseStringProperty property = (BaseStringProperty) super.clone();
        property.setValue(getValue());

        return property;
    }
}
